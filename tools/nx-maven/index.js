const { readFileSync, existsSync } = require('fs');
const { join, dirname } = require('path');

const name = 'nx-maven';

function getPomHeaderSlice(content) {
  let cleaned = content.replace(/<!--[\s\S]*?-->/g, '');
  cleaned = cleaned.replace(/<parent>[\s\S]*?<\/parent>/g, '');
  cleaned = cleaned.replace(/<dependencies>[\s\S]*?<\/dependencies>/g, '');
  cleaned = cleaned.replace(/<dependencyManagement>[\s\S]*?<\/dependencyManagement>/g, '');
  cleaned = cleaned.replace(/<build>[\s\S]*?<\/build>/g, '');
  cleaned = cleaned.replace(/<profiles>[\s\S]*?<\/profiles>/g, '');
  return cleaned;
}

function parsePomArtifactId(content) {
  const headerSlice = getPomHeaderSlice(content);
  const match = headerSlice.match(/<artifactId>\s*([^<\s]+)\s*<\/artifactId>/);
  return match ? match[1].trim() : null;
}

function parsePomPackaging(content) {
  const headerSlice = getPomHeaderSlice(content);
  const match = headerSlice.match(/<packaging>\s*([^<\s]+)\s*<\/packaging>/);
  return match ? match[1].trim() : 'jar';
}

function parsePomDescription(content) {
  const headerSlice = getPomHeaderSlice(content);
  const match = headerSlice.match(/<description>\s*([\s\S]*?)\s*<\/description>/);
  return match ? match[1].trim().replace(/\s+/g, ' ') : undefined;
}

function parsePomParent(content) {
  let cleaned = content.replace(/<!--[\s\S]*?-->/g, '');
  const parentMatch = cleaned.match(/<parent>[\s\S]*?<\/parent>/);
  if (parentMatch) {
    const match = parentMatch[0].match(/<artifactId>\s*([^<\s]+)\s*<\/artifactId>/);
    return match ? match[1].trim() : null;
  }
  return null;
}

function parsePomDependencies(content) {
  let cleaned = content.replace(/<!--[\s\S]*?-->/g, '');
  cleaned = cleaned.replace(/<dependencyManagement>[\s\S]*?<\/dependencyManagement>/g, '');
  cleaned = cleaned.replace(/<build>[\s\S]*?<\/build>/g, '');
  cleaned = cleaned.replace(/<profiles>[\s\S]*?<\/profiles>/g, '');

  const depBlocks = cleaned.match(/<dependency>[\s\S]*?<\/dependency>/g) || [];
  const result = [];
  for (const block of depBlocks) {
    const gMatch = block.match(/<groupId>\s*([^<\s]+)\s*<\/groupId>/);
    const aMatch = block.match(/<artifactId>\s*([^<\s]+)\s*<\/artifactId>/);
    if (gMatch && aMatch) {
      result.push({ groupId: gMatch[1].trim(), artifactId: aMatch[1].trim() });
    }
  }
  return result;
}

const createNodesV2 = [
  '**/pom.xml',
  (configFiles, options, context) => {
    const results = [];
    const groupId = options?.groupId ?? 'com.acme';
    const groupIdPath = groupId.split('.').join('/');
    const env = { MAVEN_OPTS: '-Dmaven.repo.local=.m2/repository' };

    for (const configFile of configFiles) {
      if (configFile.includes('node_modules') || configFile.includes('.m2')) {
        continue;
      }
      const root = context?.workspaceRoot ?? process.cwd();
      const fullPath = join(root, configFile);
      if (!existsSync(fullPath)) continue;

      const content = readFileSync(fullPath, 'utf8');
      const artifactId = parsePomArtifactId(content);
      if (!artifactId) continue;

      const packaging = parsePomPackaging(content);
      const description = parsePomDescription(content);

      const normalizedFile = configFile.replace(/\\/g, '/');
      const rawDir = dirname(normalizedFile);
      const projectRoot = rawDir === '.' ? '.' : rawDir;
      const isRoot = projectRoot === '.';
      const isPom = packaging === 'pom';
      const isSpringBootApp = !isPom && content.includes('spring-boot-maven-plugin');

      const targets = {};
      const isWin = process.platform === 'win32';
      const mvnCmd = isWin ? 'mvnw.cmd' : './mvnw';

      if (isRoot) {
        targets.build = {
          command: `${mvnCmd} install -N -f pom.xml`,
          cache: true,
          inputs: ['{workspaceRoot}/pom.xml'],
          outputs: [`{workspaceRoot}/.m2/repository/${groupIdPath}/${artifactId}`],
          options: { cwd: '.', env }
        };
        targets.lint = {
          command: `${mvnCmd} spotless:check -N -f pom.xml`,
          cache: true,
          inputs: ['{workspaceRoot}/pom.xml'],
          options: { cwd: '.', env }
        };
        targets.format = {
          command: `${mvnCmd} spotless:apply -N -f pom.xml`,
          cache: false,
          options: { cwd: '.', env }
        };
      } else if (isPom) {
        targets.build = {
          command: `${mvnCmd} install -DskipTests -f ${normalizedFile}`,
          cache: true,
          outputs: [
            `{projectRoot}/target`,
            `{workspaceRoot}/.m2/repository/${groupIdPath}/${artifactId}`
          ],
          options: { cwd: '.', env }
        };
        targets.verify = {
          command: `${mvnCmd} verify -f ${normalizedFile}`,
          cache: true,
          outputs: [
            `{projectRoot}/target/surefire-reports`,
            `{projectRoot}/target/failsafe-reports`,
            `{projectRoot}/target/site/jacoco`,
            `{projectRoot}/target/site/jacoco-aggregate`,
            `{projectRoot}/target/jacoco.exec`
          ],
          options: { cwd: '.', env }
        };
        targets.lint = {
          command: `${mvnCmd} spotless:check -f ${normalizedFile}`,
          cache: true,
          options: { cwd: '.', env }
        };
        targets.format = {
          command: `${mvnCmd} spotless:apply -f ${normalizedFile}`,
          cache: false,
          options: { cwd: '.', env }
        };
      } else {
        targets.build = {
          command: `${mvnCmd} install -DskipTests -f ${normalizedFile}`,
          cache: true,
          outputs: [
            `{projectRoot}/target`,
            `{workspaceRoot}/.m2/repository/${groupIdPath}/${artifactId}`
          ],
          options: { cwd: '.', env }
        };
        targets.test = {
          command: `${mvnCmd} test -f ${normalizedFile}`,
          cache: true,
          outputs: [
            `{projectRoot}/target/surefire-reports`,
            `{projectRoot}/target/site/jacoco`,
            `{projectRoot}/target/jacoco.exec`
          ],
          options: { cwd: '.', env }
        };
        targets.verify = {
          command: `${mvnCmd} verify -f ${normalizedFile}`,
          cache: true,
          outputs: [
            `{projectRoot}/target/surefire-reports`,
            `{projectRoot}/target/failsafe-reports`,
            `{projectRoot}/target/site/jacoco`,
            `{projectRoot}/target/jacoco.exec`
          ],
          options: { cwd: '.', env }
        };
        targets.lint = {
          command: `${mvnCmd} spotless:check -f ${normalizedFile}`,
          cache: true,
          options: { cwd: '.', env }
        };
        targets.format = {
          command: `${mvnCmd} spotless:apply -f ${normalizedFile}`,
          cache: false,
          options: { cwd: '.', env }
        };
        if (isSpringBootApp) {
          targets.serve = {
            command: `${mvnCmd} spring-boot:run -f ${normalizedFile}`,
            cache: false,
            options: { cwd: '.', env }
          };
        }
      }

      results.push([
        configFile,
        {
          projects: {
            [projectRoot]: {
              name: artifactId,
              root: projectRoot,
              projectType: isSpringBootApp ? 'application' : 'library',
              tags: [
                'lang:java',
                `packaging:${packaging}`,
                `type:${isSpringBootApp ? 'app' : packaging === 'pom' || isRoot ? 'parent' : 'lib'}`
              ],
              metadata: {
                technologies: ['maven', 'java'],
                description: description || `${artifactId} module`
              },
              targets
            }
          }
        }
      ]);
    }
    return results;
  }
];

const createDependencies = (options, context) => {
  const dependencies = [];
  const workspaceRoot = context.workspaceRoot;
  const groupId = options?.groupId ?? 'com.acme';

  const projects = Object.values(context.projects || {});
  const projectPoms = [];
  const knownProjects = new Set();

  for (const config of projects) {
    const relPom = join(config.root || '.', 'pom.xml').replace(/\\/g, '/');
    const fullPath = join(workspaceRoot, relPom);
    if (!existsSync(fullPath)) continue;
    const content = readFileSync(fullPath, 'utf8');
    const artifactId = parsePomArtifactId(content);
    if (artifactId) {
      knownProjects.add(artifactId);
      projectPoms.push({ name: artifactId, file: relPom, content });
    }
  }

  for (const item of projectPoms) {
    const { name: artifactId, file, content } = item;

    const parentArtifactId = parsePomParent(content);
    if (parentArtifactId && knownProjects.has(parentArtifactId) && parentArtifactId !== artifactId) {
      dependencies.push({
        source: artifactId,
        target: parentArtifactId,
        sourceFile: file,
        type: 'static'
      });
    }

    const deps = parsePomDependencies(content);
    for (const dep of deps) {
      if (
        (dep.groupId === groupId || dep.groupId === '${project.groupId}' || dep.groupId === '${pom.groupId}') &&
        knownProjects.has(dep.artifactId) &&
        dep.artifactId !== artifactId
      ) {
        dependencies.push({
          source: artifactId,
          target: dep.artifactId,
          sourceFile: file,
          type: 'static'
        });
      }
    }
  }

  return dependencies;
};

module.exports = {
  name,
  getPomHeaderSlice,
  parsePomArtifactId,
  parsePomPackaging,
  parsePomDescription,
  parsePomParent,
  parsePomDependencies,
  createNodesV2,
  createDependencies
};
