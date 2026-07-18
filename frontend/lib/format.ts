export function rupiah(value:number){return new Intl.NumberFormat('id-ID',{style:'currency',currency:'IDR',maximumFractionDigits:0}).format(value)}
export function time(value:string){return new Intl.DateTimeFormat('en-GB',{hour:'2-digit',minute:'2-digit'}).format(new Date(value))}
export function duration(start:string,end:string){const mins=Math.round((new Date(end).getTime()-new Date(start).getTime())/60000);return `${Math.floor(mins/60)}h ${mins%60}m`}
