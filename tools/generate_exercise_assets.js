'use strict';

const fs = require('fs');
const path = require('path');
const { execFile } = require('child_process');
const { promisify } = require('util');

const execFileAsync = promisify(execFile);
const root = path.resolve(__dirname, '..');
const mediaMap = require(path.join(root, 'seed/data/wger-video-map.json'));
const mediaOverrides = require(path.join(root, 'seed/data/exercise-media-overrides.json'));
const catalogUrl = 'https://wger.de/api/v2/exerciseinfo/?language=2&limit=1000';
const licenseUrl = 'https://creativecommons.org/licenses/by-sa/4.0/';
const licenseName = 'CC BY-SA 4.0';
const posterDirectory = path.join(root, 'app/src/main/assets/exercise-thumbnails');

async function main() {
  const response = await fetch(catalogUrl, { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('wger API returned HTTP ' + response.status);
  const result = await response.json();
  const exercisesById = new Map(result.results.map((exercise) => [exercise.id, exercise]));
  fs.mkdirSync(posterDirectory, { recursive: true });

  const media = {};
  const overriddenIds = new Set(Object.keys(mediaOverrides));
  const tasks = Object.entries(mediaMap).filter(([localId]) => !overriddenIds.has(localId)).map(([localId, wgerId]) => {
    const source = exercisesById.get(wgerId);
    if (!source) throw new Error('wger exercise ' + wgerId + ' is missing (' + localId + ')');
    const video = (source.videos || []).find((item) => item.license === 2 && item.video);
    if (!video) throw new Error('No CC BY-SA 4.0 video for ' + localId);
    const posterName = localId + '.jpg';
    const posterPath = path.join(posterDirectory, posterName);
    const sourceUrl = 'https://wger.de/en/exercise/view/' + wgerId;
    const translation = (source.translations || []).find((item) => item.language === 2);
    const sourceName = translation ? translation.name : '';
    const author = video.license_author || 'wger contributors';
    media[localId] = {
      videoUrl: video.video,
      videoType: 'mp4',
      videoDurationSeconds: Math.round(Number(video.duration) || 0),
      videoAttribution: author,
      videoLicense: licenseName,
      videoLicenseUrl: licenseUrl,
      videoSourceUrl: sourceUrl,
      videoSourceName: sourceName,
      imageUrl: 'asset://exercise-thumbnails/' + posterName,
      videoThumbnailUrl: 'asset://exercise-thumbnails/' + posterName,
      imageAttribution: author,
      imageLicense: licenseName,
      imageLicenseUrl: licenseUrl,
      imageSourceUrl: sourceUrl,
      imageSourceName: sourceName,
    };
    return { localId, url: video.video, posterPath };
  });

  for (const [localId, override] of Object.entries(mediaOverrides)) {
    const posterName = localId + '.jpg';
    const posterPath = path.join(posterDirectory, posterName);
    const { posterUrl, ...sourceData } = override;
    media[localId] = {
      ...sourceData,
      imageUrl: 'asset://exercise-thumbnails/' + posterName,
      videoThumbnailUrl: 'asset://exercise-thumbnails/' + posterName,
      imageAttribution: override.videoAttribution,
      imageLicense: override.videoLicense,
      imageLicenseUrl: override.videoLicenseUrl,
      imageSourceUrl: override.videoSourceUrl,
      imageSourceName: override.videoSourceName,
    };
    tasks.push({ localId, url: override.videoUrl, posterUrl, posterPath });
  }

  let next = 0;
  const worker = async () => {
    while (next < tasks.length) {
      const task = tasks[next++];
      if (fs.existsSync(task.posterPath) && fs.statSync(task.posterPath).size > 1000) {
        process.stdout.write('Poster already ready: ' + task.localId + '\n');
        continue;
      }
      if (task.posterUrl) {
        const response = await fetch(task.posterUrl);
        if (!response.ok) throw new Error('Poster download failed for ' + task.localId + ': HTTP ' + response.status);
        fs.writeFileSync(task.posterPath, Buffer.from(await response.arrayBuffer()));
        process.stdout.write('Poster ready: ' + task.localId + '\n');
        continue;
      }
      await execFileAsync('ffmpeg.exe', [
        '-hide_banner', '-loglevel', 'error', '-y', '-ss', '1', '-i', task.url,
        '-frames:v', '1', '-vf',
        'scale=480:270:force_original_aspect_ratio=decrease,pad=480:270:(ow-iw)/2:(oh-ih)/2',
        '-q:v', '4', task.posterPath,
      ], { windowsHide: true });
      process.stdout.write('Poster ready: ' + task.localId + '\n');
    }
  };
  await Promise.all(Array.from({ length: Math.min(4, tasks.length) }, worker));

  fs.writeFileSync(path.join(root, 'seed/data/exercise-media.json'), JSON.stringify(media, null, 2) + '\n', 'utf8');
  delete require.cache[require.resolve(path.join(root, 'seed/data/exercises.js'))];
  const catalog = require(path.join(root, 'seed/data/exercises.js'))
    .map((item) => ({ id: item.id, ...item.data }));
  fs.writeFileSync(
    path.join(root, 'app/src/main/assets/exercises.json'),
    JSON.stringify(catalog, null, 2) + '\n',
    'utf8',
  );
  process.stdout.write('Wrote ' + catalog.length + ' exercises and ' + tasks.length + ' licensed videos.\n');
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
