const { test, describe } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const os = require('os');
const plugin = require('./index.js');

function createTempWorkspace() {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'nx-maven-test-'));
}

function cleanupTempWorkspace(dir) {
  if (dir && fs.existsSync(dir)) {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

describe('nx-maven plugin unit tests', () => {

  test('1. Module POM declaring <dependencies> before <artifactId>', () => {
    const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.acme</groupId>
    <artifactId>monorepo-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>
  <dependencies>
    <dependency>
      <groupId>com.acme</groupId>
      <artifactId>common-core</artifactId>
    </dependency>
  </dependencies>
  <artifactId>my-module</artifactId>
</project>`;

    const artifactId = plugin.parsePomArtifactId(pomContent);
    assert.equal(artifactId, 'my-module');
  });

  test('2. Module omitting <groupId> inherits parent groupId and resolves correctly', () => {
    const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <parent>
    <groupId>com.acme</groupId>
    <artifactId>monorepo-parent</artifactId>
  </parent>
  <artifactId>inherited-lib</artifactId>
</project>`;

    const parentArtifactId = plugin.parsePomParent(pomContent);
    const artifactId = plugin.parsePomArtifactId(pomContent);
    assert.equal(parentArtifactId, 'monorepo-parent');
    assert.equal(artifactId, 'inherited-lib');
  });

  test('3. Dependency with ${project.groupId} or ${pom.groupId} produces inter-module dependency', () => {
    const pomContent = `<project>
      <artifactId>lib-b</artifactId>
      <dependencies>
        <dependency>
          <groupId>\${project.groupId}</groupId>
          <artifactId>lib-a</artifactId>
        </dependency>
        <dependency>
          <groupId>\${pom.groupId}</groupId>
          <artifactId>common-core</artifactId>
        </dependency>
      </dependencies>
    </project>`;

    const deps = plugin.parsePomDependencies(pomContent);
    assert.equal(deps.length, 2);
    assert.equal(deps[0].artifactId, 'lib-a');
    assert.equal(deps[1].artifactId, 'common-core');
  });

  test('4. Dependency in <dependencyManagement> only does NOT produce an edge', () => {
    const pomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <artifactId>monorepo-parent</artifactId>
  <packaging>pom</packaging>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.acme</groupId>
        <artifactId>common-core</artifactId>
        <version>1.0.0</version>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>`;

    const deps = plugin.parsePomDependencies(pomContent);
    assert.equal(deps.length, 0);
  });

  test('5. Plugin coordinate inside <build> does NOT produce an edge', () => {
    const pomContent = `<project>
      <artifactId>my-app</artifactId>
      <build>
        <plugins>
          <plugin>
            <groupId>com.acme</groupId>
            <artifactId>common-core</artifactId>
          </plugin>
        </plugins>
      </build>
    </project>`;

    const deps = plugin.parsePomDependencies(pomContent);
    assert.equal(deps.length, 0);
  });

  test('6. Aggregator POM (<packaging>pom</packaging>) produces type:parent tag and no test target', () => {
    const tmpDir = createTempWorkspace();
    try {
      const parentPomPath = path.join(tmpDir, 'pom.xml');
      const parentPomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.acme</groupId>
  <artifactId>monorepo-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
</project>`;
      fs.writeFileSync(parentPomPath, parentPomContent);

      const createNodes = plugin.createNodesV2[1];
      const nodes = createNodes(['pom.xml'], { groupId: 'com.acme' }, { workspaceRoot: tmpDir });
      assert.equal(nodes.length, 1);
      const [, config] = nodes[0];
      const project = config.projects['.'];

      assert.equal(project.name, 'monorepo-parent');
      assert.ok(project.tags.includes('type:parent'));
      assert.equal(project.targets.test, undefined);
    } finally {
      cleanupTempWorkspace(tmpDir);
    }
  });

  test('7. POM containing spring-boot-maven-plugin produces projectType application and serve target', () => {
    const tmpDir = createTempWorkspace();
    try {
      const appDir = path.join(tmpDir, 'apps', 'order-service');
      fs.mkdirSync(appDir, { recursive: true });
      const appPomPath = path.join(appDir, 'pom.xml');
      const appPomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.acme</groupId>
    <artifactId>monorepo-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>
  <artifactId>order-service</artifactId>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>`;
      fs.writeFileSync(appPomPath, appPomContent);

      const createNodes = plugin.createNodesV2[1];
      const nodes = createNodes(['apps/order-service/pom.xml'], { groupId: 'com.acme' }, { workspaceRoot: tmpDir });
      assert.equal(nodes.length, 1);
      const [, config] = nodes[0];
      const project = config.projects['apps/order-service'];

      assert.equal(project.projectType, 'application');
      assert.ok(project.tags.includes('type:app'));
      assert.ok(project.targets.serve);
      const isWin = process.platform === 'win32';
      const mvnCmd = isWin ? 'mvnw.cmd' : './mvnw';
      assert.equal(project.targets.serve.command, `${mvnCmd} spring-boot:run -f apps/order-service/pom.xml`);
    } finally {
      cleanupTempWorkspace(tmpDir);
    }
  });

  test('8. POM with <packaging>war</packaging> receives packaging:war tag and standard targets', () => {
    const tmpDir = createTempWorkspace();
    try {
      const webDir = path.join(tmpDir, 'apps', 'web-app');
      fs.mkdirSync(webDir, { recursive: true });
      const webPomPath = path.join(webDir, 'pom.xml');
      const webPomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.acme</groupId>
    <artifactId>monorepo-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>
  <artifactId>web-app</artifactId>
  <packaging>war</packaging>
</project>`;
      fs.writeFileSync(webPomPath, webPomContent);

      const createNodes = plugin.createNodesV2[1];
      const nodes = createNodes(['apps/web-app/pom.xml'], { groupId: 'com.acme' }, { workspaceRoot: tmpDir });
      assert.equal(nodes.length, 1);
      const [, config] = nodes[0];
      const project = config.projects['apps/web-app'];

      assert.equal(project.name, 'web-app');
      assert.ok(project.tags.includes('packaging:war'));
    } finally {
      cleanupTempWorkspace(tmpDir);
    }
  });

  test('9. POM missing <artifactId> defaults gracefully without crashing', () => {
    const invalidPomContent = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.acme</groupId>
</project>`;

    const artifactId = plugin.parsePomArtifactId(invalidPomContent);
    assert.equal(artifactId, null);
  });
});
