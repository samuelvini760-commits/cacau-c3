# Cacau R1 Android — primeira base nativa

Este projeto preserva o HTML original como `app/src/main/assets/index.html` e o abre em um WebView Android, com uma ponte Kotlin para Gemini e Spotify.

## Abrir e compilar
1. Extraia o ZIP no Android Studio (ou computador com Android Studio).
2. Abra a pasta `cacau-r1-android`.
3. Aguarde a sincronização do Gradle e instale no Android.
4. No menu ⋮ do app, configure Gemini e Spotify.

Este ambiente de execução não possui Android SDK/Gradle, então o APK não foi compilado nem testado aqui. O projeto contém fontes compiláveis, mas a primeira compilação real pode exigir ajustes de versão/ambiente.

## Gemini
- Crie a chave em https://aistudio.google.com/apikey e cole em **Configurar Gemini** no menu do app.
- A chave é armazenada em preferências criptografadas no aparelho, mas qualquer chave usada diretamente por um aplicativo cliente pode ser extraída durante execução. Para distribuir publicamente, migre as chamadas para Firebase AI Logic com App Check ou para um backend seguro.
- As perguntas que o cérebro local não reconhecer podem ser enviadas ao Gemini quando configurado. Os comandos locais continuam funcionando sem internet.
- Um modelo generativo offline ainda não está incluído; a versão atual oferece apenas comandos locais/offline do HTML.

## Spotify
1. Acesse https://developer.spotify.com/dashboard e crie um app.
2. Cadastre exatamente `cacaur1://spotify-callback` como Redirect URI.
3. No app Cacau, abra **Conectar Spotify**, cole o Client ID e autorize.
4. A API pode exigir Spotify Premium para controlar reprodução e um dispositivo Spotify ativo.

A versão inclui OAuth Authorization Code + PKCE, comandos básicos de reprodução e leitura da faixa atual. O código de resposta musical está em estágio inicial; os dados de ritmo/áudio avançados não são disponibilizados automaticamente pela API.

## Google services
A conexão Google Workspace ainda NÃO está implementada neste primeiro pacote. Para Gmail, Calendar e Drive, crie um projeto em https://console.cloud.google.com/, configure a tela de consentimento OAuth e ative as APIs necessárias. Cada API e escopo precisa ser implementado e autorizado separadamente. Maps e YouTube também requerem configuração e políticas próprias. Não há dados de demonstração apresentados como se fossem reais.

## Segurança / limites
- Não compartilhe seu APK com uma chave Gemini embutida; insira a chave no dispositivo para testes.
- Tokens Spotify ficam em armazenamento criptografado local; implemente refresh token/expiração robustos antes de uso contínuo.
- A Cacau não pode executar qualquer ação arbitrária em todos os serviços Google só por comando de voz: ações devem ser explicitamente implementadas, autorizadas e testadas.
