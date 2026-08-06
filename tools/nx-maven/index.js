const { readFileSync, existsSync, readdirSync } = require('fs');
const { join, dirname } = require('path');

const name = 'nx-maven';

function getPomHeaderSlice(content) {
  let cleaned = content.replace(/<!--[\s\S]*?-->/g, '');
  cleaned = cleaned.replace(/<parent>[\s\S]*?<\/parent>/g, '');
  cleaned = cleaned.replace(/<dependencies>[\s\S]*?<\/dependencies>/g, '');
  cleaned = cleaned.replace(/<dependencyManagement>[\s\S]*?<\/dependencyManagement>/g, '');
  cleaned = cleaned.replace(/<build>[\s\S]*?<\/build>/g, '');
  cleaned = cleaned.replace(/<profiles>[\s\S]*?<\/profiles>/g, '');
  cleaned = cleaned.replace(/<reporting>[\s\S]*?<\/reporting>/g, '');
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

function findPomFiles(dir, relativeDir = '') {
  let results = [];
  if (!existsSync(dir)) return results;
  const entries = readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    if (
      entry.name === 'node_modules' ||
      entry.name === '.m2' ||
      entry.name === '.nx' ||
      entry.name === 'target' ||
      entry.name.startsWith('.')
    ) {
      continue;
    }
    const full = join(dir, entry.name);
    const rel = relativeDir ? join(relativeDir, entry.name) : entry.name;
    if (entry.isDirectory()) {
      results = results.concat(findPomFiles(full, rel));
    } else if (entry.name === 'pom.xml') {
      results.push(rel.replace(/\\/g, '/'));
    }
  }
  return results;
}

const createNodesV2 = [
  '**/pom.xml',
  (configFiles, options, context) => {
    const results = [];
    const groupId = options?.groupId ?? 'com.acme';
    const groupIdPath = groupId.split('.').join('/');
    const env = {};

    for (const configFile of configFiles) {
      if (configFile.includes('node_modules') || configFile.includes('.m2')) {
        continue;
      }
      const fullPath = join(context.workspaceRoot, configFile);
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
      const isSpringBootApp = packaging !== 'pom' && content.includes('spring-boot-maven-plugin');

      const targets = {};

      if (isRoot) {
        targets.build = {
          command: `mvn install -N -f pom.xml`,
          cache: true,
          inputs: ['{workspaceRoot}/pom.xml'],
          outputs: [`{workspaceRoot}/.m2/repository/${groupIdPath}/${artifactId}`],
          options: { cwd: '.', env }
        };
        targets.lint = {
          command: `mvn spotless:check -N -f pom.xml`,
          cache: true,
          inputs: ['{workspaceRoot}/pom.xml'],
          options: { cwd: '.', env }
        };
        targets.format = {
          command: `mvn spotless:apply -N -f pom.xml`,
          cache: false,
          options: { cwd: '.', env }
        };
      } else {
        targets.build = {
          command: `mvn install -DskipTests -f ${normalizedFile}`,
          cache: true,
          outputs: [
            `{projectRoot}/target`,
            `{workspaceRoot}/.m2/repository/${groupIdPath}/${artifactId}`
          ],
          options: { cwd: '.', env }
        };
        targets.test = {
          command: `mvn test -f ${normalizedFile}`,
          cache: true,
          outputs: [
            `{projectRoot}/target/surefire-reports`,
            `{projectRoot}/target/site/jacoco`,
            `{projectRoot}/target/jacoco.exec`
          ],
          options: { cwd: '.', env }
        };
        targets.verify = {
          command: `mvn verify -f ${normalizedFile}`,
          cache: true,
          outputs: [
            `{projectRoot}/target/failsafe-reports`,
            `{projectRoot}/target/surefire-reports`,
            `{projectRoot}/target/site/jacoco`
          ],
          options: { cwd: '.', env }
        };
        targets.lint = {
          command: `mvn spotless:check -f ${normalizedFile}`,
          cache: true,
          options: { cwd: '.', env }
        };
        targets.format = {
          command: `mvn spotless:apply -f ${normalizedFile}`,
          cache: false,
          options: { cwd: '.', env }
        };
        if (isSpringBootApp) {
          targets.serve = {
            command: `mvn spring-boot:run -f ${normalizedFile}`,
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
  const pomFiles = findPomFiles(workspaceRoot);
  const knownProjects = new Set();

  for (const file of pomFiles) {
    const fullPath = join(workspaceRoot, file);
    if (!existsSync(fullPath)) continue;
    const content = readFileSync(fullPath, 'utf8');
    const artifactId = parsePomArtifactId(content);
    if (artifactId) {
      knownProjects.add(artifactId);
    }
  }

  for (const file of pomFiles) {
    const fullPath = join(workspaceRoot, file);
    if (!existsSync(fullPath)) continue;
    const content = readFileSync(fullPath, 'utf8');
    const artifactId = parsePomArtifactId(content);
    if (!artifactId) continue;

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
