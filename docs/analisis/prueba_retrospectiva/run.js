const { chromium } = require('playwright');const fs=require('fs');
(async()=>{const b=await chromium.launch();const p=await b.newPage();const errs=[];p.on('pageerror',e=>errs.push(e.message));
await p.setContent('<!doctype html><html><head><meta charset="utf-8"></head><body>'+fs.readFileSync(process.argv[2],'utf8')+'</body></html>');
const cases=JSON.parse(fs.readFileSync('backtest/minutas_cases.json','utf8'));
const out=await p.evaluate((cases)=>{const S=window.__sim;
 const MAP={R56:['R56'],A_IMP_RASPBERRY:['R02'],R16:['R16'],TABLET:['R47'],R60:['R60','R02'],R57:['R57'],A_IMP_PC_CONTROLADOR:['R43'],A_IMP_PC_BASICA:['R41'],A_SUNMI:['R44'],R51:['R51'],USB:['R16'],'IMP-AV':['R27'],'IMP-MT':['R27'],'R55 · V04':['R55'],SWITCH:['R48'],ENERGIA:['R49'],SUPRESOR:['R03'],R_ELEC:['R62'],R10:['R10'],R63:['R63'],R19:['R19'],R53:['R53'],CAM_NVR:['R53'],R54:['R54'],'R67 · V01':['R67'],R65:['R65'],'R66 · V02':['R66'],'V03 · R11':['R11'],SAT:['R46'],OBRA:['R26'],CONTRATAR:['R26'],RASP_PTO:['R49']};
 return cases.map(c=>{const x=S.runCaseSync({a:c.a});const {X,R}=S.ctx();const gen=new Set();R.it.forEach(i=>(MAP[i.id]||[]).forEach(k=>gen.add(k)));
  return {id:c.feat.id,fin:x.fin,gen:[...gen].sort(),estado:R.estado,eqs:[...new Set(R.it.filter(i=>i.eq).map(i=>i.eq))]};});},cases);
fs.writeFileSync('backtest/engine_out.json',JSON.stringify(out,null,0));console.log(out.filter(o=>!o.fin).length,'sin terminar','ERR',errs);await b.close();})();
