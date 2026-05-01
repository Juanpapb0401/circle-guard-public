# Taller 2: pruebas y lanzamiento

Para este ejercicio, debe configurar los pipelines necesarios para al menos seis de los microservicios del código disponible en:  

https://github.com/jcmunozf/circle-guard-public  

Al escoger los microservicios, considere que los escogidos se comuniquen entre sí para permitir la posterior implementación de pruebas que los involucren.  

## Actividades a considerar:

1. (10%) configurar Jenkins, Docker y Kubernetes para su utilización.  

2. (15%) Para los microservicios escogidos, debe definir los pipelines que permitan la utilización de la aplicación (dev environment) incluyendo la construcción, pruebas a diferentes niveles y deployment. 

3. (30%) En algunos de los microservicios, debe definir pruebas unitarias, integración, E2E y rendimiento que involucren los microservicios.  

   a. Al menos cinco nuevas pruebas unitarias que validen componentes individuales  
   b. Al menos cinco nuevas pruebas de integración que validen la comunicación entre servicios  
   c. Al menos cinco nuevas pruebas E2E que validen flujos completos de usuario  
   d. Pruebas de rendimiento y estrés utilizando Locust que simulen casos de uso reales del sistema  

   Todas las pruebas deben ser relevantes sobre funcionalidades existentes, ajustadas o agregadas y deben incluir análisis de las mismas.  

4. (15%) Para los microservicios escogidos, debe definir los pipelines que permitan la construcción incluyendo las pruebas de la aplicación desplegada en Kubernetes (stage environment).  

5. (15%) Para los microservicios escogidos, debe ejecutar un pipeline de despliegue, que realice la construcción incluyendo las pruebas unitarias, valide las pruebas de sistema y posteriormente despliegue la aplicación en Kubernetes. Defina todas las fases que considere adecuadas (master environment). Debe incluir la generación automática de Release Notes siguiendo las buenas prácticas de Change Management.  

6. (15%) Adecuada documentación del proceso realizado y video que permitan evidenciar todos los puntos anteriores.  

## Reporte de los resultados:

Debe entregar un documento y un video corto (máximo 8 minutos) que contenga y explique la siguiente información para cada uno de los pipelines:

- Configuración: Texto de la configuración de los pipelines, con pantallazos de configuración relevante en los mismos.  

- Resultado: pantallazos de la ejecución exitosa de los pipelines con los detalles y resultados relevantes.  

- Análisis: interpretación de los resultados de las pruebas, especialmente las de rendimiento, con métricas clave como tiempo de respuesta, throughput y tasa de errores.  

Adicionalmente, un zip con los pipelines, las pruebas implementadas y el proyecto si este fue modificado.