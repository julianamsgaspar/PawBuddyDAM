plugins {
    // Plugin para aplicações Android
    alias(libs.plugins.android.application)

    // Plugin para suporte a Kotlin no Android
    alias(libs.plugins.kotlin.android)
}

android {

    // Namespace da aplicação (package base)
    namespace = "pt.ipt.dam2025.pawbuddy"

    // Versão do SDK usada para compilação
    compileSdk = 36

    defaultConfig {
        // Identificador único da aplicação
        applicationId = "pt.ipt.dam2025.pawbuddy"

        // Versão mínima do Android suportada
        minSdk = 28

        // Versão alvo do Android
        targetSdk = 36

        // Código interno da versão (incremental)
        versionCode = 1

        // Nome visível da versão
        versionName = "1.0"

        // Classe responsável por executar testes instrumentados
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // URL base da API (valor por defeito)
        // Pode ser sobrescrito pelos buildTypes (debug / release)
        buildConfigField(
            "String",
            "BASE_URL",
            "\"https://pawbuddy-api-htdcasgkh8gphqa5.westeurope-01.azurewebsites.net/\""
        )
        buildFeatures {
            viewBinding = true
            buildConfig = true
        }

    }

    buildTypes {

        debug {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://pawbuddy-api-htdcasgkh8gphqa5.westeurope-01.azurewebsites.net/\""
            )
        }

        // Configuração específica para a versão de produção
        release {
            // Desativa a ofuscação de código (pode ser ativada futuramente)
            isMinifyEnabled = false

            // Ficheiros de regras para otimização e ofuscação
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // URL da API em ambiente de produção (Azure)
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://pawbuddy-api-htdcasgkh8gphqa5.westeurope-01.azurewebsites.net/\""
            )
        }
    }

    compileOptions {
        // Compatibilidade com Java 11
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        // Versão da JVM usada pelo Kotlin
        jvmTarget = "11"
    }

    buildFeatures {
        // Ativa o View Binding para acesso seguro às views
        viewBinding = true

        // Garante a geração da classe BuildConfig
        buildConfig = true
    }
}

dependencies {

    // Biblioteca para carregamento eficiente de imagens
    implementation(libs.glide.v4151)

    // Processador de anotações usado pelo Glide
    annotationProcessor(libs.compiler)

    // Biblioteca HTTP para comunicação com serviços remotos
    implementation(libs.okhttp)

    // Interceptor para registo (logging) de pedidos HTTP
    implementation(libs.logging.interceptor)

    // Conversor JSON para Retrofit (Gson)
    implementation(libs.converter.gson.v250)

    // Conversor para respostas em texto simples
    implementation(libs.converter.scalars)

    // Adaptador para utilização de RxJava com Retrofit
    implementation(libs.adapter.rxjava2)

    // Componentes de navegação entre fragments
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Bibliotecas CameraX para acesso à câmara
    val cameraxVersion = "1.5.1"
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.androidx.camera.extensions)

    // Layouts e componentes visuais
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Dependências base do Android
    implementation(libs.glide)
    implementation(libs.retrofit)
    implementation(libs.converterGson)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Dependências para testes unitários
    testImplementation(libs.junit)

    // Dependências para testes instrumentados
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
