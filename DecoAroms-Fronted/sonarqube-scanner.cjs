// eslint-disable-next-line no-undef
const sonarModule = require('sonarqube-scanner');
// eslint-disable-next-line no-undef
console.log('SonarQube Scanner module export:', sonarModule);

const scanner = sonarModule.default || sonarModule;

scanner(
  {
    serverUrl: "http://localhost:9000",  // URL del servidor SonarQube
    options: {
      "sonar.token": "sqp_104b2159dd5e143ca856f61536bcfb76bfa45712", // Token de autenticación de SonarQube no expirable
      "sonar.projectKey": "DecoAromas-Frontend", // Nombre de la llave que contiene el token de sonarqube
      "sonar.projectName": "DecoAromas Frontend", // Nombre del proyecto en SonarQube
      "sonar.projectDescription": "Just for demo...", // Descripción del proyecto
      "sonar.sourceEncoding": "UTF-8", // Codificación de los archivos fuente

      // Código fuente
      "sonar.sources": "src", // Directorio raíz del código fuente

      // Tests
      "sonar.tests": "src",
      "sonar.test.inclusions": "**/*.test.tsx,**/*.test.ts", // Incluir solo archivos de prueba

      // IMPORTANTE: no excluir tests
      "sonar.exclusions": "**/*.spec.tsx", // Excluir archivos de especificación si los hay

      // Cobertura
      "sonar.javascript.lcov.reportPaths": "coverage/lcov.info" // Ruta al informe de cobertura en formato lcov
    },
  },
  // eslint-disable-next-line no-undef
  () => process.exit()
);


