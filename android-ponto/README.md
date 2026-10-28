# Controle de Ponto (Android)

App Android nativo (Kotlin + Jetpack Compose) para registro de ponto.

## Funcionalidades

- **Hoje**: relógio em tempo real e botão único para registrar entrada/saída
  (alterna automaticamente, inclusive saída e volta do almoço), horas trabalhadas
  no dia com barra de progresso da jornada e saldo do mês.
- **Calendário mensal**: cada dia colorido conforme a situação — jornada ok, hora
  extra, abaixo da jornada, batida incompleta, falta ou dia abonado — com as horas
  trabalhadas. Toque em um dia para corrigir/incluir/remover batidas, marcar
  folga, feriado, férias ou atestado e escrever uma observação. Mostra o resumo
  do mês (trabalhado, esperado, saldo/banco de horas, dias trabalhados e faltas)
  e exporta o relatório em CSV (compartilhar por WhatsApp, e-mail, Drive...).
- **Ajustes**: jornada diária, tolerância e dias de trabalho da semana.

Os dados ficam salvos no próprio aparelho.

## Como obter o APK

A cada push, o GitHub Actions (workflow *App Android - Controle de Ponto*) compila
o app e publica o artefato **controle-de-ponto-apk**. Baixe o `.zip` na página da
execução, extraia o `app-debug.apk` e instale no celular (é preciso permitir
"instalar apps de fontes desconhecidas").

## Build local

Requer Android SDK (ou Android Studio). Abra a pasta `android-ponto` no Android
Studio, ou rode:

```
./gradlew assembleDebug
```

Requer Android 8.0 (API 26) ou superior.
