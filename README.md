# 🐰 AppRumi: Tu Compañero Espacial con IA 🚀

<div align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Gemini_AI-8E75B2?style=for-the-badge&logo=google-gemini&logoColor=white" alt="Gemini">
  <img src="https://img.shields.io/badge/MVVM-Blue?style=for-the-badge" alt="MVVM">
</div>

---

## 🌟 Sobre el Proyecto
**AppRumi** es una aplicación multiplataforma (Android) diseñada para transformar la productividad en una experiencia interactiva. A través de **RUMI**, un conejo espacial sarcástico integrado con la API de **Gemini 1.5**, la app motiva (o juzga) al usuario basándose en su música actual y sus hábitos pendientes.

---

## 📸 Galería de la Interfaz (UI)

### 🤖 Interacción con RUMI
RUMI utiliza arte ASCII y un tono único para comunicarse contigo. ¡Cuidado con lo que escuchas o si no haces tus tareas!

<div align="center">
  <table>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/ef174455-c937-458a-80bb-c2c33c5b466c" width="200" alt="Principal"></td>
      <td><img src="https://github.com/user-attachments/assets/3c839002-266c-4146-814c-3bc16559abe5" width="200" alt="Chat"></td>
      <td><img src="https://github.com/user-attachments/assets/3b8d9e2d-f26e-4c37-bf21-1fe25ea48f45" width="200" alt="Acariciar"></td>
    </tr>
    <tr>
      <td align="center"><b>Inicio</b></td>
      <td align="center"><b>Chat IA</b></td>
      <td align="center"><b>Interacción</b></td>
    </tr>
  </table>
</div>

### 📅 Gestión de Hábitos y Tareas
Organiza tu día a día mientras la IA supervisa tu progreso.

<div align="center">
  <table>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/ad06b886-799a-43d9-928b-8e881bc71d76" width="200" alt="Lista"></td>
      <td><img src="https://github.com/user-attachments/assets/0e370407-8708-41cb-a940-e25632ff5266" width="200" alt="Nuevo Habito"></td>
    </tr>
    <tr>
      <td align="center"><b>Mis Hábitos</b></td>
      <td align="center"><b>Editor</b></td>
    </tr>
  </table>
</div>

### 🎵 Biblioteca Musical
Integración con música para que RUMI reaccione según el ritmo de tu día.

<div align="center">
  <table>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/e7c8e0bf-4fde-479a-a7f6-42e9d51d8f05" width="200" alt="Biblioteca"></td>
      <td><img src="https://github.com/user-attachments/assets/37e36ac3-2e50-42c4-8ee7-21e95c02927a" width="200" alt="Player"></td>
    </tr>
    <tr>
      <td align="center"><b>Biblioteca</b></td>
      <td align="center"><b>Reproductor</b></td>
    </tr>
  </table>
</div>

---

## 🛠️ Stack Tecnológico y Arquitectura
El proyecto sigue los principios de **Clean Architecture** y **MVVM**:

* **UI:** Jetpack Compose para una interfaz moderna y fluida.
* **IA:** Google Generative AI SDK (Gemini).
* **Persistencia:** Room Database para hábitos y mensajes.
* **Inyección de Dependencias:** Gestión manual de Factories.
* **Seguridad:** Implementación de `BuildConfig` y `local.properties` para protección de API Keys.

---


## 🚀 Instalación
1. Clona el repo: `git clone https://github.com/SrNiock/AppRumi.git`
2. Crea tu `local.properties` en la raíz.
3. Añade: `GEMINI_API_KEY=tu_clave_de_google_ai_studio`
4. ¡Dale a **Run** en Android Studio!
