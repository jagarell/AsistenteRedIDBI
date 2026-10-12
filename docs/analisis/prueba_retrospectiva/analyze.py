import json,sys
from collections import defaultdict
C=json.load(open('minutas_cases.json')); E={o['id']:o for o in json.load(open('engine_out.json'))}
def avail(rule,f,a):
    eq=bool(f['equipoCaja']); imp=bool(f['impresoras'])
    return {'R56':'conexion' in f or 'P17' in a,'R02':eq and bool(f['prep']),'R47':eq,'R16':imp,'R43':imp and eq,'R51':imp,
            'R11':any(k.startswith('P23#') for k in a),'R53':f['camaras'],'R10':True,'R63':f['rubro']=='RETAIL','R26':f['estado'] in ('EN_OBRA','POR_INAUGURAR')}.get(rule,False)
RULES=['R56','R02','R47','R16','R43','R51','R11','R53','R10','R63','R26']
st={r:defaultdict(int) for r in RULES}; rows=[]
for c in C:
    f=c['feat']; a=c['a']; real=set(f['real']); gen=set(E[f['id']]['gen'])
    comp=[r for r in RULES if avail(r,f,a)]
    for r in comp:
        st[r]['TP' if r in real and r in gen else 'FN' if r in real else 'FP' if r in gen else 'TN']+=1
    agree=sum(1 for r in comp if (r in real)==(r in gen))
    rows.append(dict(id=f['id'],nombre=f['nombre'],rubro=f['rubro'],comp=len(comp),agree=agree,falto=sorted(r for r in comp if r in real and r not in gen),demas=sorted(r for r in comp if r in gen and r not in real)))
tot=sum(r['comp'] for r in rows); ag=sum(r['agree'] for r in rows)
print('comparables',tot,'coinciden',ag,round(100*ag/tot,1))
for r in RULES:
    s=st[r]; n=sum(s.values())
    if n: print(r,dict(s),round(100*(s['TP']+s['TN'])/n),'%')
for r in rows:
    if r['falto'] or r['demas']: print(r)
json.dump({'rows':rows,'st':{k:dict(v) for k,v in st.items()},'tot':tot,'ag':ag},open(sys.argv[1],'w'),ensure_ascii=False)
