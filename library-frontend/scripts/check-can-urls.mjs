// <Can url="..."> 와 can("...") 에 쓴 URL 리터럴이 백엔드 action_urls 시드에 실제로 있는지 검사한다.
// 프론트는 등록 패턴 문자열과 "정확히 일치"만 보므로, 오타가 나면 버튼이 조용히 사라진다 — 빌드 전에 잡는다.
// 사용: node scripts/check-can-urls.mjs  (npm run check:can, npm run build 에서 자동 실행)
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const srcDir = join(root, 'src');
const seedFile = join(root, '..', 'library-backend', 'src', 'main', 'resources', 'data.sql');

const CAN_LITERAL = /<Can\s+url="([^"]+)"/g;
const CAN_CALL = /\bcan\(\s*['"]([^'"]+)['"]\s*\)/g;
const CAN_DYNAMIC = /<Can\s+url=\{/g;
const SEED_URL = /^INSERT INTO action_urls .*?SELECT a\.id, '([A-Z]+)', '([^']+)'/;

/** src 아래 .ts/.tsx 파일 목록 */
function sourceFiles(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      return sourceFiles(path);
    }
    return /\.(ts|tsx)$/.test(name) ? [path] : [];
  });
}

/** 백엔드 시드의 "METHOD /pattern" 집합 */
function seededUrls() {
  const urls = new Set();
  for (const line of readFileSync(seedFile, 'utf8').split(/\r?\n/)) {
    const match = SEED_URL.exec(line);
    if (match) {
      urls.add(`${match[1]} ${match[2]}`);
    }
  }
  return urls;
}

/** 파일별 URL 리터럴과 동적 url 사용 위치 */
function collectUsages(files) {
  const usages = [];
  const dynamic = [];
  for (const file of files) {
    const text = readFileSync(file, 'utf8');
    for (const regex of [CAN_LITERAL, CAN_CALL]) {
      for (const match of text.matchAll(regex)) {
        usages.push({ file: relative(root, file), url: match[1] });
      }
    }
    if (CAN_DYNAMIC.test(text)) {
      dynamic.push(relative(root, file));
    }
    CAN_DYNAMIC.lastIndex = 0;
  }
  return { usages, dynamic };
}

const seeded = seededUrls();
if (seeded.size === 0) {
  console.error(`[check:can] 시드에서 action_urls 를 찾지 못했습니다: ${seedFile}`);
  process.exit(1);
}
const { usages, dynamic } = collectUsages(sourceFiles(srcDir));
const missing = usages.filter(({ url }) => !seeded.has(url));

if (dynamic.length > 0) {
  console.error('[check:can] <Can url={...}> 동적 값은 검사할 수 없습니다. 문자열 리터럴을 쓰세요:');
  dynamic.forEach((file) => console.error(`  - ${file}`));
}
if (missing.length > 0) {
  console.error('[check:can] 백엔드 action_urls 시드에 없는 URL:');
  missing.forEach(({ file, url }) => console.error(`  - ${url}  (${file})`));
}
if (dynamic.length > 0 || missing.length > 0) {
  process.exit(1);
}
console.log(`[check:can] OK — ${usages.length}개 사용처가 시드 URL ${seeded.size}개와 일치합니다.`);
