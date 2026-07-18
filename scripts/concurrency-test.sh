#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESULT_FILE="$ROOT_DIR/docs/concurrency-test-result.txt"
exec > >(tee "$RESULT_FILE") 2>&1

BASE_URL="${BASE_URL:-http://localhost:8080}"
N="${N:-50}"
FLIGHT_ID="${FLIGHT_ID:-1}"
SEAT_NUMBER="${SEAT_NUMBER:-12F}"
COMPOSE_DIR="$ROOT_DIR/infra"
WORK_DIR="$(mktemp -d)"
TOKENS_FILE="$WORK_DIR/tokens.txt"
RESP_DIR="$WORK_DIR/responses"
mkdir -p "$RESP_DIR"
trap 'rm -rf "$WORK_DIR"' EXIT

psql_tripbook() {
  docker exec -i tripbook-postgres psql -U tripbook -d tripbook "$@"
}

wait_for_nginx() {
  for _ in {1..60}; do
    if curl -fsS "$BASE_URL/api/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "ERROR: nginx/backend did not become ready" >&2
  return 1
}

extract_token() {
  node --input-type=module -e 'let s=""; process.stdin.on("data", d => s += d); process.stdin.on("end", () => console.log(JSON.parse(s).token));'
}

seat_id="$(psql_tripbook -At -c "SELECT id FROM flight_seats WHERE flight_id=$FLIGHT_ID AND seat_number='$SEAT_NUMBER';")"
if [[ -z "$seat_id" ]]; then
  echo "ERROR: seat not found for flight_id=$FLIGHT_ID seat_number=$SEAT_NUMBER" >&2
  exit 1
fi

wait_for_nginx

echo "== TripBook concurrency test =="
echo "timestamp: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "base_url: $BASE_URL"
echo "n: $N"
echo "flight_id: $FLIGHT_ID"
echo "seat_number: $SEAT_NUMBER"
echo "flight_seat_id: $seat_id"

echo "== Reset seat and related bookings =="
psql_tripbook -v ON_ERROR_STOP=1 -c "DELETE FROM bookings WHERE flight_seat_id=$seat_id; UPDATE flight_seats SET status='AVAILABLE' WHERE id=$seat_id; SELECT id, seat_number, status FROM flight_seats WHERE id=$seat_id;"

echo "== Register/login $N users =="
: > "$TOKENS_FILE"
run_id="$(date +%s)-$$"
for i in $(seq 1 "$N"); do
  email="race-${run_id}-${i}@tripbook.com"
  password="password123"
  curl -fsS -X POST "$BASE_URL/api/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$password\",\"fullName\":\"Race User $i\"}" >/dev/null
  token="$(curl -fsS -X POST "$BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}" | extract_token)"
  printf '%s|%s\n' "$i" "$token" >> "$TOKENS_FILE"
done
wc -l "$TOKENS_FILE"

echo "== Fire $N simultaneous booking requests =="
cat "$TOKENS_FILE" | xargs -P "$N" -I {} bash -c '
  line="$1"
  idx="${line%%|*}"
  token="${line#*|}"
  out="'"$RESP_DIR"'/response-${idx}.txt"
  curl -sS -o "${out}.body" -D "${out}.headers" \
    -w "status=%{http_code} time_total=%{time_total}\n" \
    -X POST "'"$BASE_URL"'/api/bookings/flight" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "{\"flightId\":'"$FLIGHT_ID"',\"seatNumber\":\"'"$SEAT_NUMBER"'\",\"passengerName\":\"Race User ${idx}\"}" > "${out}.meta"
  instance="$(awk "BEGIN{IGNORECASE=1} /^X-Instance-Id:/{gsub(/\r/, \"\"); print \$2}" "${out}.headers")"
  status="$(awk -F "[= ]" "{print \$2}" "${out}.meta")"
  time_total="$(awk -F "[= ]" "{print \$4}" "${out}.meta")"
  printf "request=%02d status=%s instance=%s time_total=%s\n" "$idx" "$status" "$instance" "$time_total"
' _ {}

echo "== Status summary =="
status_201="$(grep -h 'status=201' "$RESP_DIR"/*.meta | wc -l | tr -d ' ')"
status_409="$(grep -h 'status=409' "$RESP_DIR"/*.meta | wc -l | tr -d ' ')"
status_500="$(grep -h 'status=500' "$RESP_DIR"/*.meta | wc -l | tr -d ' ')"
other_statuses="$(awk -F '[= ]' '{print $2}' "$RESP_DIR"/*.meta | sort | uniq -c)"
echo "201_count=$status_201"
echo "409_count=$status_409"
echo "500_count=$status_500"
echo "all_statuses:"
echo "$other_statuses"

echo "== Instances served =="
awk 'BEGIN{IGNORECASE=1} /^X-Instance-Id:/{gsub(/\r/, ""); print $2}' "$RESP_DIR"/*.headers | sort | uniq -c

echo "== Slowest request =="
awk -F '[= ]' '{print FILENAME, $4, $2}' "$RESP_DIR"/*.meta | sort -k2,2nr | head -1 | awk '{print "file=" $1 " time_total=" $2 " status=" $3}'

echo "== DB verification =="
booking_count="$(psql_tripbook -At -c "SELECT count(*) FROM bookings WHERE flight_seat_id=$seat_id;")"
psql_tripbook -c "SELECT count(*) AS booking_count FROM bookings WHERE flight_seat_id=$seat_id; SELECT status FROM flight_seats WHERE id=$seat_id;"
echo "booking_count=$booking_count"

echo "== Assertions =="
failed=0
if [[ "$status_201" != "1" ]]; then echo "ASSERT_FAIL: expected exactly 1 HTTP 201"; failed=1; fi
if [[ "$status_409" != "$((N - 1))" ]]; then echo "ASSERT_FAIL: expected exactly $((N - 1)) HTTP 409"; failed=1; fi
if [[ "$status_500" != "0" ]]; then echo "ASSERT_FAIL: expected 0 HTTP 500"; failed=1; fi
if [[ "$booking_count" != "1" ]]; then echo "ASSERT_FAIL: expected exactly 1 booking row"; failed=1; fi
if ! awk 'BEGIN{IGNORECASE=1} /^X-Instance-Id:/{gsub(/\r/, ""); print $2}' "$RESP_DIR"/*.headers | sort -u | grep -qx 'backend-1'; then echo "ASSERT_FAIL: backend-1 did not serve any request"; failed=1; fi
if ! awk 'BEGIN{IGNORECASE=1} /^X-Instance-Id:/{gsub(/\r/, ""); print $2}' "$RESP_DIR"/*.headers | sort -u | grep -qx 'backend-2'; then echo "ASSERT_FAIL: backend-2 did not serve any request"; failed=1; fi

if [[ "$failed" -ne 0 ]]; then
  echo "RESULT=FAIL"
  exit 1
fi

echo "RESULT=PASS"
echo "saved_result_file: $RESULT_FILE"
