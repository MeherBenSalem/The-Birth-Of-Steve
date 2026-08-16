/**
 * Publish loader jars to Modrinth and CurseForge from this machine.
 *
 * Modrinth: one listed version per jar (never multiple files on one version).
 * version_number: {mod}+mc{minecraft}+{loader}  e.g. 0.6.0+mc26.1.2+fabric
 *
 * CurseForge: one file upload per jar (unchanged).
 *
 * Usage:
 *   gradlew.bat collectJars
 *   # load MODRINTH_TOKEN, CURSEFORGE_TOKEN from Desktop\local.env or env
 *   node tools/publish-local.mjs . 0.6.0
 *
 * CI sets JAR_LIST (newline-separated paths) instead of reading build/libs.
 */
import fs from 'fs';
import path from 'path';

const repo = path.resolve(process.argv[2] || '.');
const version = process.argv[3];
const modrinthOnly = process.argv.includes('--modrinth-only');
if (!version) {
  console.error('Usage: node tools/publish-local.mjs <repo> <version> [--modrinth-only]');
  process.exit(1);
}

const modrinthId = process.env.MODRINTH_ID || 'gKOBlOap';
const curseforgeId = process.env.CURSEFORGE_ID || '1621994';

const changelog = fs
  .readFileSync(path.join(repo, 'CHANGELOG.md'), 'utf8')
  .split(/^## /m)
  .find((section) => section.startsWith(version))
  ?.replace(new RegExp(`^${version.replace(/\./g, '\\.')}[^\\n]*\\n+`), '')
  ?.trim() || `Release ${version}`;

function loaderOf(jar) {
  const n = path.basename(jar).toLowerCase();
  if (n.includes('neoforge')) return 'neoforge';
  if (n.includes('quilt')) return 'quilt';
  if (n.includes('fabric')) return 'fabric';
  if (n.includes('forge')) return 'forge';
  return 'fabric';
}

function mcOf(jar) {
  const match = path.basename(jar).match(/tbos-\w+-([\d.]+)-/);
  if (!match) throw new Error('Could not parse Minecraft version from ' + jar);
  return match[1];
}

function modrinthVersionNumber(mcVersion, loader) {
  return `${version}+mc${mcVersion}+${loader}`;
}

function modrinthVersionName(mcVersion, loader) {
  return `${version} (${loader}) for Minecraft ${mcVersion}`;
}

async function fetchCurseForgeVersions() {
  const res = await fetch('https://minecraft.curseforge.com/api/game/versions', {
    headers: { 'X-Api-Token': process.env.CURSEFORGE_TOKEN },
  });
  const text = await res.text();
  if (!res.ok) throw new Error('CurseForge versions ' + res.status + ' ' + text.slice(0, 300));
  return JSON.parse(text);
}

function curseForgeIdOf(flat, want) {
  const hit = flat.find((v) => v.name.toLowerCase() === want.toLowerCase());
  if (!hit) throw new Error('No CurseForge version id for ' + want);
  return hit.id;
}

function curseForgeLoaderName(loader) {
  const names = { fabric: 'Fabric', forge: 'Forge', neoforge: 'NeoForge', quilt: 'Quilt' };
  const hit = names[loader];
  if (!hit) throw new Error('Unsupported loader ' + loader);
  return hit;
}

function curseForgeGameVersions(flat, mcVersion, loader) {
  return [
    curseForgeIdOf(flat, mcVersion),
    curseForgeIdOf(flat, curseForgeLoaderName(loader)),
    curseForgeIdOf(flat, 'Client'),
    curseForgeIdOf(flat, 'Server'),
  ];
}

async function publishModrinth(jar, mcVersion, loader) {
  const body = {
    name: modrinthVersionName(mcVersion, loader),
    version_number: modrinthVersionNumber(mcVersion, loader),
    changelog,
    dependencies: [],
    game_versions: [mcVersion],
    version_type: 'release',
    loaders: [loader],
    featured: false,
    status: 'listed',
    project_id: modrinthId,
    file_parts: ['file'],
    primary_file: 'file',
  };
  const form = new FormData();
  form.append('data', JSON.stringify(body));
  form.append('file', new Blob([fs.readFileSync(jar)]), path.basename(jar));
  const res = await fetch('https://api.modrinth.com/v2/version', {
    method: 'POST',
    headers: { Authorization: process.env.MODRINTH_TOKEN },
    body: form,
  });
  const text = await res.text();
  if (!res.ok) {
    if (res.status === 400 && text.includes('version_number')) {
      console.log('Modrinth skip (already exists)', modrinthVersionNumber(mcVersion, loader));
      return;
    }
    throw new Error('Modrinth ' + res.status + ' ' + text.slice(0, 500));
  }
  console.log('Modrinth OK', modrinthVersionNumber(mcVersion, loader), text.slice(0, 120));
}

async function publishCurseForge(jar, cfVersions, mcVersion, loader) {
  const meta = {
    changelog,
    changelogType: 'markdown',
    displayName: path.basename(jar),
    gameVersions: curseForgeGameVersions(cfVersions, mcVersion, loader),
    releaseType: 'release',
  };
  const form = new FormData();
  form.append('metadata', JSON.stringify(meta));
  form.append('file', new Blob([fs.readFileSync(jar)]), path.basename(jar));
  const res = await fetch(
    `https://minecraft.curseforge.com/api/projects/${curseforgeId}/upload-file`,
    { method: 'POST', headers: { 'X-Api-Token': process.env.CURSEFORGE_TOKEN }, body: form },
  );
  const text = await res.text();
  if (!res.ok) throw new Error('CurseForge ' + res.status + ' ' + text.slice(0, 500));
  console.log('CurseForge OK', path.basename(jar), text.slice(0, 120));
}

async function publishJar(jar, cfVersions) {
  const mcVersion = mcOf(jar);
  const loader = loaderOf(jar);
  console.log(`\n== ${path.basename(jar)} ==`);
  await publishModrinth(jar, mcVersion, loader);
  if (!modrinthOnly) {
    await publishCurseForge(jar, cfVersions, mcVersion, loader);
  }
}

function discoverJars() {
  if (process.env.JAR_LIST) {
    return process.env.JAR_LIST.split(/\n/).map((s) => s.trim()).filter(Boolean);
  }
  const jarDir = path.join(repo, 'build', 'libs');
  return fs
    .readdirSync(jarDir)
    .filter((name) => name.endsWith('.jar') && name.includes(`-${version}.jar`))
    .map((name) => path.join(jarDir, name))
    .sort();
}

if (!process.env.MODRINTH_TOKEN || !process.env.CURSEFORGE_TOKEN) {
  throw new Error('Set MODRINTH_TOKEN and CURSEFORGE_TOKEN before publishing');
}

const jars = discoverJars();
if (!jars.length) throw new Error(`No ${version} jars to publish`);

const cfVersions = modrinthOnly ? null : await fetchCurseForgeVersions();
for (const jar of jars) {
  await publishJar(jar, cfVersions);
}

console.log('\nAll uploads complete.');
