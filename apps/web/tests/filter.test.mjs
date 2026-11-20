import test from 'node:test';
import assert from 'node:assert/strict';
import { matches, selectApps } from '../src/filter.mjs';

const rows = [
  {name:'GoreeCloud Notebook', summary:'Personal notes', category:'Productivity',platforms:['web','linux']},
  {name:'GoreeCloud Photos', summary:'Image library', category:'Media',platforms:['android','web']}
];

test('searches names, summaries and categories case-insensitively',()=> {
  assert.equal(matches(rows[0],'NOTES'),true);
  assert.equal(matches(rows[1],'Productivity'),false);
});
test('filters category and platform simultaneously',()=>{
  assert.deepEqual(selectApps(rows,'','Media','android'),[rows[1]]);
  assert.deepEqual(selectApps(rows,'','Media','linux'),[]);
});
