---
name: Workflow preferences - no ejecutar sin aprobación
description: El usuario exige aprobación explícita antes de cualquier cambio o ejecución
type: feedback
originSessionId: 85c732f8-fa44-4fc6-8b4e-2f7067e34a54
---
Nunca crear archivos, directorios, ni ejecutar comandos sin que el usuario lo apruebe explícitamente.

**Why:** El usuario lo pidió directamente: "Nunca hagas cambios sin mi aprobación explícita."

**How to apply:** Flujo obligatorio en cada sesión:
1. Claude analiza y explica el plan con detalle
2. Se discute si hay ajustes
3. Usuario dice explícitamente que aprueba (ej. "dale", "hazlo", "sí me parece")
4. Solo entonces Claude ejecuta

Esto aplica a: escribir archivos, crear directorios, correr comandos bash, commits, push a GitHub. Cualquier cosa.
