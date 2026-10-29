# Como enviar para o GitHub pelo celular (sem pastas)

Todos os arquivos desta pasta ficam soltos (sem subpastas) para o GitHub aceitar o upload pelo celular.
O workflow `build-apk.yml` coloca cada arquivo na pasta certa do projeto Android automaticamente antes de compilar.

1. Crie um repositório no GitHub.
2. **Add file > Upload files** e envie TODOS os arquivos desta pasta, EXCETO `build-apk.yml`.
3. **Add file > Create new file**. No campo do nome digite: `.github/workflows/build-apk.yml`
   (ao digitar as barras `/` o GitHub cria as pastas sozinho). Cole o conteúdo do arquivo `build-apk.yml` e toque em **Commit changes**.
4. Abra a aba **Actions** > **Build Cacau R1 APK** > **Run workflow**.
5. Ao terminar com sucesso, baixe o artefato `Cacau-R1-debug-APK`.

Observação: o APK só estará disponível se o build terminar com sucesso (não foi compilado/testado aqui).
