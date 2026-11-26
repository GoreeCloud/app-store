import test from 'node:test';
import assert from 'node:assert/strict';
import { loadApps } from '../src/catalog.mjs';

const row = i => ({slug:'app-'+i,name:'App '+i,summary:'Sample catalog row',category:'Utilities',
  platforms:['web'],status:'source-only',releases:[],source_url:'https://github.com/GoreeCloud/app-'+i});
function server(rows, change = p => p) {
  return async (url, options) => {
    assert.equal(options.redirect, 'error');
    const offset = Number(new URL(url,'https://example.test').searchParams.get('offset'));
    return {ok:true,json:async()=>change({total:rows.length,offset,limit:100,items:rows.slice(offset,offset+100)})};
  };
}
test('loads more than 100 records without dropping entries', async () => {
  const rows = Array.from({length:231},(_,i)=>row(i));
  assert.deepEqual(await loadApps(server(rows)),rows);
});
test('rejects changed counts and inconsistent pages', async () => {
  const rows = Array.from({length:101},(_,i)=>row(i));
  await assert.rejects(loadApps(server(rows,p=>p.offset?{...p,total:102}:p)));
  await assert.rejects(loadApps(server(rows,p=>({...p,items:[]}))));
});
test('rejects duplicate slugs, unsafe URLs and unsigned package claims', async () => {
  await assert.rejects(loadApps(server([row(1),row(1)])));
  await assert.rejects(loadApps(server([{...row(1),source_url:'javascript:alert(1)'}])));
  await assert.rejects(loadApps(server([{...row(1),status:'packages-available'}])));
});
test('empty catalog succeeds and oversized catalog fails', async () => {
  assert.deepEqual(await loadApps(server([])),[]);
  await assert.rejects(loadApps(server([],p=>({...p,total:1001}))));
});
