# Relatorio de consumo de tokens por workspace/sessao

> **Recorte:** apenas sessoes cuja primeira requisicao ocorreu a partir de 2026/09/02 00:00 (15 de 161 sessoes com uso registrado).
> Fonte: `~/.claude/projects/**/*.jsonl` - toda entrada com `message.usage`, deduplicada por `requestId`.
> **Workspace = `cwd` exato da sessao.** Subpastas nao sao consolidadas na raiz: cada diretorio de trabalho aparece como workspace proprio.
> Uma sessao e atribuida ao `cwd` de sua primeira requisicao.
> Custo e **estimativa** a preco de tabela da API Anthropic: cache write (5m) = 1,25x input, cache read = 0,1x input.
> Uso via assinatura Claude Code nao e faturado assim - o valor serve como proxy comparavel de volume.

---

## 1. Inventario de `C:\Users\User\.claude\projects\`

### `C--Users-User`

3 arquivo(s) - 0.08 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `1f23eae5-2371-49d4-be27-a537c1fb1af9.jsonl` | 27.597 B | 2026-09-02 12:18 |
| `3f48a946-0bee-4c3b-95a0-8fe8e728749f.jsonl` | 17.411 B | 2026-09-02 13:30 |
| `ebc3d08f-9145-491c-a2fa-c881b61d37d8.jsonl` | 42.523 B | 2026-09-02 12:10 |

### `C--Users-User-AppData-Roaming-Claude-scratch-workspaces-8ea5fdd5-ef38-4e15-b97c-6d37cee37f2c-c07d2e3c-a6c3-4c16-a34b-b1c8a7b0af97-scratch-2026-09-01-4fb476`

1 arquivo(s) - 0.05 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `eb65fa31-b302-4c3f-a41c-ebc71d646f5b.jsonl` | 53.922 B | 2026-09-03 03:14 |

### `D--CobraKai-SDD`

6 arquivo(s) - 0.00 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `memory\MEMORY.md` | 689 B | 2026-05-29 12:19 |
| `memory\feedback_destructive_actions.md` | 732 B | 2026-05-29 12:19 |
| `memory\feedback_verbosity.md` | 687 B | 2026-05-29 12:19 |
| `memory\project_banestes_sdd.md` | 1.479 B | 2026-05-29 13:00 |
| `memory\reference_spec_kit.md` | 546 B | 2026-05-29 12:19 |
| `memory\user_profile.md` | 810 B | 2026-05-29 12:19 |

### `D--CobraKai-SDD-meuSDD-SSPDD`

12 arquivo(s) - 8.33 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `1feda861-e298-4758-99d1-4f5a03bd04f6.jsonl` | 1.070.100 B | 2026-08-22 12:15 |
| `347bf843-8c8e-408c-b607-2b2338d2ac1f.jsonl` | 2.629.334 B | 2026-08-22 22:30 |
| `3d133337-4012-43bf-be21-1306c3bf5073.jsonl` | 84.203 B | 2026-08-22 22:51 |
| `430454c0-d1d5-4cd2-88e9-42de8ef02028.jsonl` | 563.294 B | 2026-08-22 13:55 |
| `48f5f2ef-dfdd-413d-b3fc-661de7506152.jsonl` | 542.959 B | 2026-08-22 13:14 |
| `4f94a010-0519-4119-a4b1-0e85af481580.jsonl` | 319.163 B | 2026-08-22 12:36 |
| `7241cef7-fba9-44bf-803d-14f2de54e6f6.jsonl` | 694.121 B | 2026-08-22 13:42 |
| `94a08245-7c66-49ac-8011-0e0bdc86fcb6.jsonl` | 1.151.099 B | 2026-08-22 11:45 |
| `9b144941-a47f-4690-a930-1b91704cb5e4.jsonl` | 405.334 B | 2026-08-22 13:27 |
| `b83468ef-e56a-4366-a290-998df9ea7add.jsonl` | 494.845 B | 2026-08-22 13:23 |
| `da667891-c47f-4b1b-bece-5e693bcd17b2.jsonl` | 402.923 B | 2026-08-22 23:36 |
| `dfa3fc5d-3aed-49d5-a1f1-69d41fab7746.jsonl` | 374.555 B | 2026-08-22 12:30 |

### `D--CobraKai-bcorp`

5 arquivo(s) - 0.01 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `memory\MEMORY.md` | 748 B | 2026-05-15 17:57 |
| `memory\feedback_permissions.md` | 783 B | 2026-05-15 10:18 |
| `memory\project_bcorp_roadmap.md` | 1.374 B | 2026-04-27 09:54 |
| `memory\project_pagsal_gaps.md` | 2.294 B | 2026-05-04 09:56 |
| `memory\project_sip512_corrections.md` | 2.693 B | 2026-05-15 17:57 |

### `D--DEV-Projects-CRUDAO-experimentos-quarta-versao`

42 arquivo(s) - 30.25 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `08270caa-b563-4ae5-adfd-dd28234a12b5.jsonl` | 814.103 B | 2026-08-28 14:07 |
| `193af071-571a-4258-bb95-33dabf05d947.jsonl` | 791.098 B | 2026-08-31 10:10 |
| `19cf57af-2650-4978-b4c9-39a2394cdcdb.jsonl` | 1.120.323 B | 2026-08-29 15:47 |
| `1ce88fa9-37e2-4889-a8bf-f7a509143415.jsonl` | 5.031.385 B | 2026-09-01 13:36 |
| `44196b29-af86-4f83-bb2d-0ab536c6b617.jsonl` | 2.616.967 B | 2026-08-29 17:41 |
| `5b06b87d-f46c-4047-a457-227478fe0911.jsonl` | 1.378.044 B | 2026-08-28 16:10 |
| `69e45d9e-ace1-4cc5-9cf2-8df859942867.jsonl` | 1.141.013 B | 2026-08-31 12:22 |
| `7157b2a7-8a5c-429d-9135-9b66ff508166.jsonl` | 1.209.140 B | 2026-08-31 16:14 |
| `77e22f44-d7dd-40cf-95b4-a9d9258bcf8f.jsonl` | 897.294 B | 2026-08-29 16:06 |
| `833d0525-4b8f-4598-983a-3a8596cb0463.jsonl` | 2.257.834 B | 2026-09-01 10:27 |
| `83dc4c7d-5180-4200-b46f-a6575e771e66.jsonl` | 2.601 B | 2026-08-29 15:47 |
| `996da2dd-d173-4207-af41-0fffcc24e861.jsonl` | 1.181.757 B | 2026-08-28 13:55 |
| `b40342c5-40a6-4d39-8d40-e85654b3648a.jsonl` | 833.284 B | 2026-08-29 07:46 |
| `b9ad07ed-a764-45cf-bb44-f6d480906d44.jsonl` | 2.220.993 B | 2026-08-28 15:07 |
| `cf0e95eb-262f-4707-8a8d-75de7b3bbcc4.jsonl` | 931.751 B | 2026-08-28 15:33 |
| `dc2f0baa-d459-43b9-8282-ff08e2afc6a0.jsonl` | 2.863.504 B | 2026-08-29 17:40 |
| `dd610331-7c19-46d3-a706-50b910fb3651.jsonl` | 1.202.819 B | 2026-08-28 15:52 |
| `e2050d2a-1aa4-428f-afda-f2c5930af99e.jsonl` | 2.290.095 B | 2026-08-31 15:31 |
| `e2ecedb2-6094-43cb-a081-320ef60b7cda.jsonl` | 726.944 B | 2026-09-01 11:18 |
| `e864c19f-e025-4fba-9e9c-e0a5fbe2eaa1.jsonl` | 10.415 B | 2026-08-28 11:53 |
| `fb8881f6-2a20-4cbc-ae7c-c50cba0d941c.jsonl` | 992.946 B | 2026-08-30 22:02 |
| `08270caa-b563-4ae5-adfd-dd28234a12b5\tool-results\hook-toolu_01HQ9heiXbUhh7t8nw3HH8kx-2-additionalContext.txt` | 13.550 B | 2026-08-28 14:05 |
| `08270caa-b563-4ae5-adfd-dd28234a12b5\tool-results\hook-toolu_01N3kiNZ6Kz3QuvC6YbpJv4e-2-additionalContext.txt` | 10.551 B | 2026-08-28 14:03 |
| `193af071-571a-4258-bb95-33dabf05d947\subagents\agent-ad89d9c0a573c597f.jsonl` | 224.620 B | 2026-08-31 09:26 |
| `193af071-571a-4258-bb95-33dabf05d947\subagents\agent-ad89d9c0a573c597f.meta.json` | 139 B | 2026-08-31 09:23 |
| `19cf57af-2650-4978-b4c9-39a2394cdcdb\custom-title.json` | 37 B | 2026-08-29 15:47 |
| `5b06b87d-f46c-4047-a457-227478fe0911\subagents\agent-aabd588f3c55e79a3.jsonl` | 147.845 B | 2026-08-28 16:03 |
| `5b06b87d-f46c-4047-a457-227478fe0911\subagents\agent-aabd588f3c55e79a3.meta.json` | 152 B | 2026-08-28 16:02 |
| `833d0525-4b8f-4598-983a-3a8596cb0463\subagents\agent-ad1d1c22f551519e3.jsonl` | 248.389 B | 2026-09-01 08:55 |
| `833d0525-4b8f-4598-983a-3a8596cb0463\subagents\agent-ad1d1c22f551519e3.meta.json` | 129 B | 2026-09-01 08:51 |
| `996da2dd-d173-4207-af41-0fffcc24e861\tool-results\hook-toolu_0131TgWoYaJRrpC4DfsiVWU2-2-additionalContext.txt` | 10.550 B | 2026-08-28 13:47 |
| `b9ad07ed-a764-45cf-bb44-f6d480906d44\tool-results\hook-toolu_015VxeUeEWYk1SFGx9TdJSxB-2-additionalContext.txt` | 10.551 B | 2026-08-28 14:26 |
| `b9ad07ed-a764-45cf-bb44-f6d480906d44\tool-results\hook-toolu_01RjvGhAGYee3j9ken7RgKoV-2-additionalContext.txt` | 10.552 B | 2026-08-28 14:53 |
| `b9ad07ed-a764-45cf-bb44-f6d480906d44\tool-results\hook-toolu_01UATKgbBqVPs7EokBGPLGih-2-additionalContext.txt` | 10.552 B | 2026-08-28 15:05 |
| `b9ad07ed-a764-45cf-bb44-f6d480906d44\tool-results\hook-toolu_01YRSLE9Xbk97mLZXJfDUYQx-2-additionalContext.txt` | 10.552 B | 2026-08-28 14:30 |
| `b9ad07ed-a764-45cf-bb44-f6d480906d44\tool-results\toolu_01UV29p68NxmbAJ6w59p3UrE.txt` | 30.196 B | 2026-08-28 14:17 |
| `cf0e95eb-262f-4707-8a8d-75de7b3bbcc4\tool-results\hook-toolu_01HrenqV9pzh4AMZiS21uhDA-2-additionalContext.txt` | 10.551 B | 2026-08-28 15:27 |
| `e2050d2a-1aa4-428f-afda-f2c5930af99e\custom-title.json` | 43 B | 2026-08-31 15:31 |
| `e2050d2a-1aa4-428f-afda-f2c5930af99e\subagents\agent-a04ecac4ba02053aa.jsonl` | 252.598 B | 2026-08-31 13:09 |
| `e2050d2a-1aa4-428f-afda-f2c5930af99e\subagents\agent-a04ecac4ba02053aa.meta.json` | 145 B | 2026-08-31 13:08 |
| `fb8881f6-2a20-4cbc-ae7c-c50cba0d941c\subagents\agent-a68d135593a9cb7b7.jsonl` | 211.281 B | 2026-08-30 19:37 |
| `fb8881f6-2a20-4cbc-ae7c-c50cba0d941c\subagents\agent-a68d135593a9cb7b7.meta.json` | 129 B | 2026-08-30 19:34 |

### `D--DEV-Projects-CRUDAO-primeira-versao`

7 arquivo(s) - 1.82 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `48e2cdd6-8b22-4b10-a6c0-38b374ed3786.jsonl` | 62.742 B | 2026-08-27 11:31 |
| `4f8e4d5a-ddd9-46c4-b704-8accbb55e0be.jsonl` | 296.328 B | 2026-08-27 14:46 |
| `98260d10-74cc-432c-ba5c-5f8bae6e0666.jsonl` | 353.525 B | 2026-08-27 09:03 |
| `c009ee40-c87e-427b-905e-230878b3e10f.jsonl` | 232.666 B | 2026-08-25 08:05 |
| `d17e93e1-43cb-449a-bba8-624d58ee162f.jsonl` | 100.568 B | 2026-08-24 15:00 |
| `4f8e4d5a-ddd9-46c4-b704-8accbb55e0be\tool-results\btkgge5kg.txt` | 834.418 B | 2026-08-27 13:14 |
| `4f8e4d5a-ddd9-46c4-b704-8accbb55e0be\tool-results\toolu_01XidQBF1qCaSzc2HRPzPrt1.txt` | 27.417 B | 2026-08-27 13:14 |

### `D--DEV-Projects-spacescape-app`

3 arquivo(s) - 0.00 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `memory\MEMORY.md` | 366 B | 2026-05-10 11:10 |
| `memory\feedback_update_progress_files.md` | 1.216 B | 2026-05-09 21:35 |
| `memory\project_game_architecture.md` | 1.403 B | 2026-05-10 11:10 |

### `D--DEV-Projects-ultimateforce`

2 arquivo(s) - 0.00 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `memory\MEMORY.md` | 133 B | 2026-05-09 16:00 |
| `memory\project_state.md` | 1.317 B | 2026-05-09 16:00 |

### `d--CobraKai-SDD-meuSDD`

11 arquivo(s) - 13.43 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `0509d591-3c4f-45e1-b623-479cef95fa6f.jsonl` | 154.057 B | 2026-08-24 16:31 |
| `09a2c3c4-0e8a-49a3-9258-e4364e674847.jsonl` | 904.018 B | 2026-08-26 08:32 |
| `433fb038-4776-4460-9e53-017cc2045155.jsonl` | 366.416 B | 2026-08-18 11:19 |
| `446352aa-81ef-4c98-8a27-775b48d8ed1a.jsonl` | 144.741 B | 2026-08-22 14:32 |
| `4d90d4ce-793b-44c1-8548-d9288b98c7fe.jsonl` | 387.279 B | 2026-08-25 08:28 |
| `9e1117c5-3187-4dcb-9710-db2603cfa385.jsonl` | 11.599.705 B | 2026-08-21 15:59 |
| `bccf6f17-3b1d-4fa7-9c28-79aa931f5112.jsonl` | 479.501 B | 2026-08-27 11:31 |
| `f239798b-c99e-4726-93d9-5a3c59614065.jsonl` | 13.160 B | 2026-08-13 17:06 |
| `09a2c3c4-0e8a-49a3-9258-e4364e674847\tool-results\hook-toolu_01QSmu6ZUGrFvK4NCG6rTfw9-2-additionalContext.txt` | 10.781 B | 2026-08-26 07:39 |
| `bccf6f17-3b1d-4fa7-9c28-79aa931f5112\tool-results\hook-toolu_012YKCMy9TrG3YMYeHfSx1r5-2-additionalContext.txt` | 10.977 B | 2026-08-26 14:11 |
| `bccf6f17-3b1d-4fa7-9c28-79aa931f5112\tool-results\hook-toolu_01Pbc1CejBqVGJu44u3XMGPN-2-additionalContext.txt` | 11.335 B | 2026-08-26 14:11 |

### `d--DEV-Projects-2030-countdown`

8 arquivo(s) - 5.49 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `26a7d5c2-49e4-4ae4-9887-d59106a33ddb.jsonl` | 2.301 B | 2026-08-29 09:31 |
| `391152dc-05c0-4899-a291-a01a9113ce44.jsonl` | 665.983 B | 2026-08-29 09:31 |
| `71a1f2c2-c2f3-440d-a54e-27f3115fb9e2.jsonl` | 3.925.719 B | 2026-08-15 20:49 |
| `b0d420d1-7bfc-4650-924a-ca5f2f0ca778.jsonl` | 818.045 B | 2026-08-29 09:31 |
| `b2d1e25a-ecfc-40c3-9876-46d9c28f6b59.jsonl` | 1.232 B | 2026-08-08 10:49 |
| `ca20c438-9aaf-4c13-8119-15577369d405.jsonl` | 314.169 B | 2026-08-21 09:00 |
| `71a1f2c2-c2f3-440d-a54e-27f3115fb9e2\tool-results\hook-toolu_01CjYGnpcDAhK4Z97d2sSwHf-2-additionalContext.txt` | 19.599 B | 2026-08-15 20:43 |
| `71a1f2c2-c2f3-440d-a54e-27f3115fb9e2\tool-results\hook-toolu_01V1ZYKRKBXQAae88VUfURhB-2-additionalContext.txt` | 13.195 B | 2026-08-15 20:42 |

### `d--DEV-Projects-CRUDAO`

207 arquivo(s) - 56.99 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `1003972e-5e9f-4145-ae6c-584c40759e9c.jsonl` | 354.897 B | 2026-08-25 07:57 |
| `1581c184-1848-4e03-ac42-2e148c6e4252.jsonl` | 285.764 B | 2026-08-26 10:42 |
| `231d1b81-866c-4f44-91cb-b50cca43fb4c.jsonl` | 433.725 B | 2026-08-24 13:11 |
| `231d77b7-0e7f-4076-8c27-19c1dce4e376.jsonl` | 420.035 B | 2026-08-24 14:35 |
| `28059ea6-8669-4b59-8f08-da38e2211bb9.jsonl` | 1.273.863 B | 2026-08-22 22:29 |
| `2956840c-6e0c-4785-8a56-7cd9daab712f.jsonl` | 391.967 B | 2026-08-24 13:20 |
| `2a750777-328f-4cc8-aaac-4668d34785db.jsonl` | 738.640 B | 2026-08-24 14:35 |
| `32242c3b-0544-4eab-bb28-213962c31c01.jsonl` | 2.318.905 B | 2026-08-23 21:51 |
| `32b44e89-846d-45fd-bea2-97f37fb3b046.jsonl` | 1.001.566 B | 2026-08-22 22:42 |
| `3bcef85c-0f64-4222-a798-ba07380efa80.jsonl` | 50.843 B | 2026-08-23 00:00 |
| `3c6f1484-0629-4584-b122-786c0a802c30.jsonl` | 2.355.985 B | 2026-08-26 09:39 |
| `41bc1f63-5749-4005-b367-12e4e77edaeb.jsonl` | 24.157 B | 2026-08-27 11:31 |
| `43563bfc-7e69-444f-a223-dd15f58ed6cf.jsonl` | 3.206.243 B | 2026-08-22 20:58 |
| `4c360599-d6b4-4085-88e7-1d52facc8fcf.jsonl` | 424.115 B | 2026-08-25 09:53 |
| `4f924211-99f3-4286-8183-01e1c9fbbf65.jsonl` | 744.604 B | 2026-08-25 21:59 |
| `54f404be-6b99-4e3a-bb82-b3744a14feb6.jsonl` | 570.103 B | 2026-08-23 21:53 |
| `56368b6c-9892-44b4-8398-9f1a78ed383e.jsonl` | 828.923 B | 2026-08-25 14:27 |
| `594252fa-de6e-41ec-a7e4-4cefe0866fc0.jsonl` | 1.018.588 B | 2026-08-26 10:35 |
| `5c918c3d-ace6-4cf6-ac03-c4caaef7044d.jsonl` | 1.028.132 B | 2026-08-24 14:35 |
| `5e1dfa0b-9a9e-4014-9325-3e864009bf7c.jsonl` | 797.589 B | 2026-08-26 09:54 |
| `5f6d8005-7ef8-4f9a-9afa-98c7f08cc203.jsonl` | 3.853.634 B | 2026-08-23 18:29 |
| `6f007b5e-b437-43ca-8867-2102d2ac8222.jsonl` | 1.567.295 B | 2026-08-23 21:53 |
| `779692c1-3e47-4552-b9d6-620525174cf5.jsonl` | 282.538 B | 2026-08-26 11:39 |
| `7be9d0cf-4b3d-4e7f-9295-b49b9be4351e.jsonl` | 3.046.221 B | 2026-08-26 14:31 |
| `8bda705e-488c-45a7-a4d0-a62993ccd467.jsonl` | 3.563 B | 2026-08-23 21:00 |
| `8c1931de-8eef-4cae-965d-c180fbf5df7a.jsonl` | 510.253 B | 2026-08-24 14:35 |
| `8c665379-ef9c-46b9-8b64-edee24d01849.jsonl` | 825.693 B | 2026-08-23 18:58 |
| `8c95a185-41fd-43b5-9930-4187463aac32.jsonl` | 1.263.757 B | 2026-08-25 15:48 |
| `8f8c3778-a801-4e2d-b2e1-6d8f59f94a5c.jsonl` | 727.171 B | 2026-08-26 10:11 |
| `90d53361-0a51-4bd5-bba0-8c5e4f2585c7.jsonl` | 375.867 B | 2026-08-25 09:46 |
| `91e615b7-d2fb-45cb-b4a6-8206697acb0b.jsonl` | 1.947.506 B | 2026-08-24 09:58 |
| `9723eaad-4123-44f0-bf69-8c0677211db6.jsonl` | 38.418 B | 2026-08-24 14:57 |
| `9a917140-a646-4207-9d9c-b6d06e06b6c4.jsonl` | 854.855 B | 2026-08-25 11:52 |
| `9d09b399-11f7-4220-863a-96083c4c7b69.jsonl` | 928.976 B | 2026-08-26 07:07 |
| `a3d02f1c-9665-457b-96eb-b3586594ecfb.jsonl` | 1.278.129 B | 2026-08-22 22:56 |
| `a628bbd9-3ddb-40a6-a220-0a85b12384c0.jsonl` | 593.595 B | 2026-08-25 10:35 |
| `a65323fa-822c-40f6-b92b-19ad5c9c2147.jsonl` | 411.211 B | 2026-08-26 11:49 |
| `a6b2adf9-9418-4cc0-9750-113a2831c09d.jsonl` | 203.930 B | 2026-08-25 07:28 |
| `aa701d1d-5baf-4e54-aed9-69272a137016.jsonl` | 610.176 B | 2026-08-26 07:42 |
| `b1e0e82a-cd8d-4d2a-a916-a4155466fbfc.jsonl` | 1.014.300 B | 2026-08-26 08:16 |
| `b5f99fc7-2fd8-4865-8c43-f4fc04bfdfca.jsonl` | 25.526 B | 2026-08-28 16:10 |
| `b7eae549-4f8e-4190-94b3-38cd90612de4.jsonl` | 1.781.361 B | 2026-08-25 11:22 |
| `c4a15089-cf55-41e5-8adb-4e840197cd42.jsonl` | 127.608 B | 2026-08-24 15:15 |
| `c4f32886-db50-4f7e-ae51-fec94543c521.jsonl` | 1.850 B | 2026-08-23 21:53 |
| `c576d953-a39a-4787-b617-42d9d80371b2.jsonl` | 1.037.286 B | 2026-08-25 09:23 |
| `c6f2295b-0f71-45da-b221-183da2c0e442.jsonl` | 856.575 B | 2026-08-25 08:09 |
| `cb79e807-7410-4697-a11f-df2dc66ff7dc.jsonl` | 285.560 B | 2026-08-24 11:30 |
| `d13ec7a0-bc3b-4ce0-86b0-ee665fd03e0e.jsonl` | 447.269 B | 2026-08-26 11:16 |
| `d14d7711-2d79-4e10-a509-0c78e63ad67b.jsonl` | 895.807 B | 2026-08-24 09:09 |
| `d58b040d-995f-488d-820d-5e245b553384.jsonl` | 813.317 B | 2026-08-24 09:09 |
| `d7fd03bd-4dd5-4f73-a414-8845fa1c044b.jsonl` | 791.476 B | 2026-08-25 14:51 |
| `d9604faa-5c12-4111-8767-e2578d11e051.jsonl` | 1.085.260 B | 2026-08-26 07:07 |
| `dad47f57-87a4-4438-98a0-b1dbe887516a.jsonl` | 519.715 B | 2026-08-26 07:51 |
| `dcbcb145-e5a1-49a7-bbe0-c28c946737be.jsonl` | 754.271 B | 2026-08-25 10:21 |
| `dd663b10-dee1-4a0a-afdc-ef783ba90bb1.jsonl` | 1.066.943 B | 2026-08-25 16:20 |
| `e3d7a1ce-8313-4616-9e1e-3ca5696797ec.jsonl` | 552.576 B | 2026-08-24 09:56 |
| `eac3dd1f-8d54-4925-85a0-ca39954ed2d2.jsonl` | 276.125 B | 2026-08-24 10:56 |
| `ee16515e-6e4a-472a-b378-8ecdbfb2c7e7.jsonl` | 359.079 B | 2026-08-26 11:00 |
| `f5647515-d2c8-4f0a-9b54-195802ff66d7.jsonl` | 143.831 B | 2026-08-24 10:27 |
| `f5934f68-c27e-4977-8e4c-a403dc4a40c0.jsonl` | 865.637 B | 2026-08-24 14:35 |
| `f78c9b3a-f2b5-417c-b416-b0d926d9295f.jsonl` | 303.070 B | 2026-08-24 14:35 |
| `f8be558a-5f85-447b-9057-0d68823f64bb.jsonl` | 149.141 B | 2026-08-24 10:44 |
| `ff14d81a-a2a5-4e4c-9c44-6d63e86deea4.jsonl` | 470.013 B | 2026-08-25 13:26 |
| `1003972e-5e9f-4145-ae6c-584c40759e9c\subagents\agent-ab644f9d98e188c36.jsonl` | 129.751 B | 2026-08-25 07:47 |
| `1003972e-5e9f-4145-ae6c-584c40759e9c\subagents\agent-ab644f9d98e188c36.meta.json` | 133 B | 2026-08-25 07:44 |
| `1003972e-5e9f-4145-ae6c-584c40759e9c\tool-results\hook-toolu_01QDmtLen8jjGMVpcExUWWUA-2-additionalContext.txt` | 13.744 B | 2026-08-25 07:50 |
| `1581c184-1848-4e03-ac42-2e148c6e4252\subagents\agent-a2b0c5c1d18d04c78.jsonl` | 87.800 B | 2026-08-26 10:41 |
| `1581c184-1848-4e03-ac42-2e148c6e4252\subagents\agent-a2b0c5c1d18d04c78.meta.json` | 142 B | 2026-08-26 10:40 |
| `231d1b81-866c-4f44-91cb-b50cca43fb4c\subagents\agent-ae4c1c5aace693fef.jsonl` | 81.732 B | 2026-08-24 12:27 |
| `231d1b81-866c-4f44-91cb-b50cca43fb4c\subagents\agent-ae4c1c5aace693fef.meta.json` | 141 B | 2026-08-24 12:26 |
| `231d77b7-0e7f-4076-8c27-19c1dce4e376\subagents\agent-ae7724338fbd2fa1d.jsonl` | 97.627 B | 2026-08-24 13:32 |
| `231d77b7-0e7f-4076-8c27-19c1dce4e376\subagents\agent-ae7724338fbd2fa1d.meta.json` | 129 B | 2026-08-24 13:31 |
| `28059ea6-8669-4b59-8f08-da38e2211bb9\subagents\agent-a201dcfba267a0437.jsonl` | 183.204 B | 2026-08-22 22:17 |
| `28059ea6-8669-4b59-8f08-da38e2211bb9\subagents\agent-a201dcfba267a0437.meta.json` | 138 B | 2026-08-22 22:15 |
| `2956840c-6e0c-4785-8a56-7cd9daab712f\subagents\agent-a8799abcb7c3b9348.jsonl` | 109.733 B | 2026-08-24 13:19 |
| `2956840c-6e0c-4785-8a56-7cd9daab712f\subagents\agent-a8799abcb7c3b9348.meta.json` | 129 B | 2026-08-24 13:18 |
| `2a750777-328f-4cc8-aaac-4668d34785db\subagents\agent-a2b4ebc7890a2488c.jsonl` | 132.152 B | 2026-08-24 12:15 |
| `2a750777-328f-4cc8-aaac-4668d34785db\subagents\agent-a2b4ebc7890a2488c.meta.json` | 141 B | 2026-08-24 12:14 |
| `32242c3b-0544-4eab-bb28-213962c31c01\subagents\agent-a2d4a2acba95e394b.jsonl` | 39.103 B | 2026-08-23 20:20 |
| `32242c3b-0544-4eab-bb28-213962c31c01\subagents\agent-a2d4a2acba95e394b.meta.json` | 145 B | 2026-08-23 20:19 |
| `32242c3b-0544-4eab-bb28-213962c31c01\subagents\agent-a5ba3d5b59e31222d.jsonl` | 91.798 B | 2026-08-23 20:20 |
| `32242c3b-0544-4eab-bb28-213962c31c01\subagents\agent-a5ba3d5b59e31222d.meta.json` | 145 B | 2026-08-23 20:19 |
| `32242c3b-0544-4eab-bb28-213962c31c01\subagents\agent-a63976b5365172d0a.jsonl` | 58.452 B | 2026-08-23 20:20 |
| `32242c3b-0544-4eab-bb28-213962c31c01\subagents\agent-a63976b5365172d0a.meta.json` | 157 B | 2026-08-23 20:20 |
| `32242c3b-0544-4eab-bb28-213962c31c01\tool-results\hook-toolu_013sHMR9HVGejgj9TyXtZ9Da-2-additionalContext.txt` | 16.827 B | 2026-08-23 20:14 |
| `32242c3b-0544-4eab-bb28-213962c31c01\tool-results\hook-toolu_014HAESGCHKJ4QR7CkM7tFGS-2-additionalContext.txt` | 20.538 B | 2026-08-23 20:43 |
| `32242c3b-0544-4eab-bb28-213962c31c01\tool-results\hook-toolu_019qb9a9m7bVZS1Ju9nXJuQQ-2-additionalContext.txt` | 20.116 B | 2026-08-23 20:34 |
| `32242c3b-0544-4eab-bb28-213962c31c01\tool-results\hook-toolu_01FZZSvHT6fgCFrCJCxPa7bJ-2-additionalContext.txt` | 18.087 B | 2026-08-23 20:23 |
| `32242c3b-0544-4eab-bb28-213962c31c01\tool-results\hook-toolu_01Hju83f9jrytxjMgzVLqC5r-2-additionalContext.txt` | 18.659 B | 2026-08-23 20:41 |
| `32242c3b-0544-4eab-bb28-213962c31c01\tool-results\hook-toolu_01XA4BkuaQ8pryWVcuXevVQw-2-additionalContext.txt` | 30.907 B | 2026-08-23 20:22 |
| `32242c3b-0544-4eab-bb28-213962c31c01\tool-results\hook-toolu_01XubMZFjAtvkFqJgKfv3P5B-2-additionalContext.txt` | 11.466 B | 2026-08-23 19:46 |
| `32b44e89-846d-45fd-bea2-97f37fb3b046\subagents\agent-a86bd6736b9804b2b.jsonl` | 203.054 B | 2026-08-22 21:22 |
| `32b44e89-846d-45fd-bea2-97f37fb3b046\subagents\agent-a86bd6736b9804b2b.meta.json` | 140 B | 2026-08-22 21:20 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a3c6657819220aaaf.jsonl` | 301.478 B | 2026-08-26 09:09 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a3c6657819220aaaf.meta.json` | 144 B | 2026-08-26 09:03 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a520e88e004193ed6.jsonl` | 120.939 B | 2026-08-26 08:14 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a520e88e004193ed6.meta.json` | 143 B | 2026-08-26 08:12 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a6530ac8896e96316.jsonl` | 5.263 B | 2026-08-26 07:54 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a6530ac8896e96316.meta.json` | 143 B | 2026-08-26 07:54 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a69ced65bdab5fe94.jsonl` | 6.511 B | 2026-08-26 08:30 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a69ced65bdab5fe94.meta.json` | 139 B | 2026-08-26 08:30 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a8f02e982187fc6f6.jsonl` | 4.483 B | 2026-08-26 07:54 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-a8f02e982187fc6f6.meta.json` | 145 B | 2026-08-26 07:54 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-ae5dcf37aa8d8dd46.jsonl` | 5.510 B | 2026-08-26 08:31 |
| `3c6f1484-0629-4584-b122-786c0a802c30\subagents\agent-ae5dcf37aa8d8dd46.meta.json` | 137 B | 2026-08-26 08:30 |
| `4f924211-99f3-4286-8183-01e1c9fbbf65\subagents\agent-a4d26fb47e5da1202.jsonl` | 148.183 B | 2026-08-25 13:11 |
| `4f924211-99f3-4286-8183-01e1c9fbbf65\subagents\agent-a4d26fb47e5da1202.meta.json` | 143 B | 2026-08-25 13:09 |
| `54f404be-6b99-4e3a-bb82-b3744a14feb6\subagents\agent-aa72cf135c4bc269f.jsonl` | 99.214 B | 2026-08-23 21:24 |
| `54f404be-6b99-4e3a-bb82-b3744a14feb6\subagents\agent-aa72cf135c4bc269f.meta.json` | 159 B | 2026-08-23 21:23 |
| `56368b6c-9892-44b4-8398-9f1a78ed383e\subagents\agent-a841876a37624db69.jsonl` | 181.920 B | 2026-08-25 14:24 |
| `56368b6c-9892-44b4-8398-9f1a78ed383e\subagents\agent-a841876a37624db69.meta.json` | 129 B | 2026-08-25 14:22 |
| `594252fa-de6e-41ec-a7e4-4cefe0866fc0\subagents\agent-a4496a4e9a138554e.jsonl` | 13.174 B | 2026-08-26 10:15 |
| `594252fa-de6e-41ec-a7e4-4cefe0866fc0\subagents\agent-a4496a4e9a138554e.meta.json` | 133 B | 2026-08-26 10:14 |
| `594252fa-de6e-41ec-a7e4-4cefe0866fc0\subagents\agent-a798e31f823df1995.jsonl` | 15.130 B | 2026-08-26 10:15 |
| `594252fa-de6e-41ec-a7e4-4cefe0866fc0\subagents\agent-a798e31f823df1995.meta.json` | 149 B | 2026-08-26 10:15 |
| `594252fa-de6e-41ec-a7e4-4cefe0866fc0\tool-results\bfrvh5psh.txt` | 34.606 B | 2026-08-26 10:34 |
| `5c918c3d-ace6-4cf6-ac03-c4caaef7044d\subagents\agent-a1a7f9dbd4f9676f9.jsonl` | 136.972 B | 2026-08-24 13:49 |
| `5c918c3d-ace6-4cf6-ac03-c4caaef7044d\subagents\agent-a1a7f9dbd4f9676f9.meta.json` | 133 B | 2026-08-24 13:47 |
| `5e1dfa0b-9a9e-4014-9325-3e864009bf7c\subagents\agent-ac2fa1b07bf54220d.jsonl` | 230.300 B | 2026-08-26 09:50 |
| `5e1dfa0b-9a9e-4014-9325-3e864009bf7c\subagents\agent-ac2fa1b07bf54220d.meta.json` | 129 B | 2026-08-26 09:47 |
| `5f6d8005-7ef8-4f9a-9afa-98c7f08cc203\subagents\agent-a39a98323243bf4be.jsonl` | 88.241 B | 2026-08-23 18:10 |
| `5f6d8005-7ef8-4f9a-9afa-98c7f08cc203\subagents\agent-a39a98323243bf4be.meta.json` | 144 B | 2026-08-23 18:09 |
| `5f6d8005-7ef8-4f9a-9afa-98c7f08cc203\subagents\agent-a7d4240c72c112776.jsonl` | 127.269 B | 2026-08-22 23:39 |
| `5f6d8005-7ef8-4f9a-9afa-98c7f08cc203\subagents\agent-a7d4240c72c112776.meta.json` | 144 B | 2026-08-22 23:38 |
| `6f007b5e-b437-43ca-8867-2102d2ac8222\subagents\agent-a290649f726fcca90.jsonl` | 163.452 B | 2026-08-23 21:09 |
| `6f007b5e-b437-43ca-8867-2102d2ac8222\subagents\agent-a290649f726fcca90.meta.json` | 146 B | 2026-08-23 21:06 |
| `779692c1-3e47-4552-b9d6-620525174cf5\subagents\agent-acd12f9441bd54336.jsonl` | 78.256 B | 2026-08-26 11:25 |
| `779692c1-3e47-4552-b9d6-620525174cf5\subagents\agent-acd12f9441bd54336.meta.json` | 129 B | 2026-08-26 11:24 |
| `7be9d0cf-4b3d-4e7f-9295-b49b9be4351e\subagents\agent-a7abd173e8d90e8c9.jsonl` | 145.349 B | 2026-08-26 11:55 |
| `7be9d0cf-4b3d-4e7f-9295-b49b9be4351e\subagents\agent-a7abd173e8d90e8c9.meta.json` | 144 B | 2026-08-26 11:53 |
| `7be9d0cf-4b3d-4e7f-9295-b49b9be4351e\tool-results\batvz56cg.txt` | 30.421 B | 2026-08-26 13:51 |
| `8c1931de-8eef-4cae-965d-c180fbf5df7a\subagents\agent-abe1c63941cf86e2f.jsonl` | 119.430 B | 2026-08-24 11:54 |
| `8c1931de-8eef-4cae-965d-c180fbf5df7a\subagents\agent-abe1c63941cf86e2f.meta.json` | 138 B | 2026-08-24 11:54 |
| `8c1931de-8eef-4cae-965d-c180fbf5df7a\subagents\agent-ad21cf91a78fc4598.jsonl` | 80.335 B | 2026-08-24 11:54 |
| `8c1931de-8eef-4cae-965d-c180fbf5df7a\subagents\agent-ad21cf91a78fc4598.meta.json` | 139 B | 2026-08-24 11:53 |
| `8c665379-ef9c-46b9-8b64-edee24d01849\subagents\agent-a856eb4e8aede1018.jsonl` | 152.408 B | 2026-08-23 18:51 |
| `8c665379-ef9c-46b9-8b64-edee24d01849\subagents\agent-a856eb4e8aede1018.meta.json` | 139 B | 2026-08-23 18:49 |
| `8c95a185-41fd-43b5-9930-4187463aac32\subagents\agent-a828a95d8f9ad264c.jsonl` | 139.533 B | 2026-08-25 15:41 |
| `8c95a185-41fd-43b5-9930-4187463aac32\subagents\agent-a828a95d8f9ad264c.meta.json` | 159 B | 2026-08-25 15:39 |
| `8f8c3778-a801-4e2d-b2e1-6d8f59f94a5c\subagents\agent-aa7db35fbc39752ce.jsonl` | 187.662 B | 2026-08-26 10:05 |
| `8f8c3778-a801-4e2d-b2e1-6d8f59f94a5c\subagents\agent-aa7db35fbc39752ce.meta.json` | 129 B | 2026-08-26 10:02 |
| `90d53361-0a51-4bd5-bba0-8c5e4f2585c7\subagents\agent-a89eb18b32292f3b1.jsonl` | 94.794 B | 2026-08-25 09:33 |
| `90d53361-0a51-4bd5-bba0-8c5e4f2585c7\subagents\agent-a89eb18b32292f3b1.meta.json` | 135 B | 2026-08-25 09:32 |
| `9a917140-a646-4207-9d9c-b6d06e06b6c4\subagents\agent-ace0d735d86021121.jsonl` | 109.395 B | 2026-08-25 11:52 |
| `9a917140-a646-4207-9d9c-b6d06e06b6c4\subagents\agent-ace0d735d86021121.meta.json` | 142 B | 2026-08-25 11:51 |
| `9d09b399-11f7-4220-863a-96083c4c7b69\subagents\agent-ac07f16d722cf9b2a.jsonl` | 158.890 B | 2026-08-25 13:48 |
| `9d09b399-11f7-4220-863a-96083c4c7b69\subagents\agent-ac07f16d722cf9b2a.meta.json` | 129 B | 2026-08-25 13:47 |
| `a3d02f1c-9665-457b-96eb-b3586594ecfb\tool-results\b0w6wefam.txt` | 30.005 B | 2026-08-22 22:47 |
| `a628bbd9-3ddb-40a6-a220-0a85b12384c0\subagents\agent-a5b404ce7b0935112.jsonl` | 115.057 B | 2026-08-25 10:33 |
| `a628bbd9-3ddb-40a6-a220-0a85b12384c0\subagents\agent-a5b404ce7b0935112.meta.json` | 141 B | 2026-08-25 10:32 |
| `aa701d1d-5baf-4e54-aed9-69272a137016\subagents\agent-a1aa80bb5e21195cb.jsonl` | 124.992 B | 2026-08-26 07:38 |
| `aa701d1d-5baf-4e54-aed9-69272a137016\subagents\agent-a1aa80bb5e21195cb.meta.json` | 139 B | 2026-08-26 07:37 |
| `b1e0e82a-cd8d-4d2a-a916-a4155466fbfc\subagents\agent-ab0f12aa8be3c36d9.jsonl` | 93.563 B | 2026-08-25 15:07 |
| `b1e0e82a-cd8d-4d2a-a916-a4155466fbfc\subagents\agent-ab0f12aa8be3c36d9.meta.json` | 147 B | 2026-08-25 15:05 |
| `b7eae549-4f8e-4190-94b3-38cd90612de4\subagents\agent-af217290298f6ec84.jsonl` | 172.907 B | 2026-08-25 10:46 |
| `b7eae549-4f8e-4190-94b3-38cd90612de4\subagents\agent-af217290298f6ec84.meta.json` | 137 B | 2026-08-25 10:44 |
| `b7eae549-4f8e-4190-94b3-38cd90612de4\tool-results\b4pyg7vzn.txt` | 47.125 B | 2026-08-25 10:54 |
| `c576d953-a39a-4787-b617-42d9d80371b2\subagents\agent-afaf0323c225416cc.jsonl` | 76.538 B | 2026-08-25 08:36 |
| `c576d953-a39a-4787-b617-42d9d80371b2\subagents\agent-afaf0323c225416cc.meta.json` | 144 B | 2026-08-25 08:35 |
| `c576d953-a39a-4787-b617-42d9d80371b2\subagents\agent-afd2b3b0f6aed06eb.jsonl` | 139.794 B | 2026-08-25 08:37 |
| `c576d953-a39a-4787-b617-42d9d80371b2\subagents\agent-afd2b3b0f6aed06eb.meta.json` | 144 B | 2026-08-25 08:35 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-a13809ada40f833ca.jsonl` | 88.357 B | 2026-08-25 08:06 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-a13809ada40f833ca.meta.json` | 144 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-a41c13eca35fd7b5c.jsonl` | 43.927 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-a41c13eca35fd7b5c.meta.json` | 143 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-a625a4afab6795b07.jsonl` | 28.951 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-a625a4afab6795b07.meta.json` | 145 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-abe425720a6da9da0.jsonl` | 64.547 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-abe425720a6da9da0.meta.json` | 143 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-ace88aaf51c69f439.jsonl` | 34.165 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\subagents\agent-ace88aaf51c69f439.meta.json` | 139 B | 2026-08-25 08:05 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\tool-results\hook-toolu_011S9AxC2ziFN6PwArCScfiW-2-additionalContext.txt` | 29.054 B | 2026-08-25 08:08 |
| `c6f2295b-0f71-45da-b221-183da2c0e442\tool-results\hook-toolu_01MJJ8wnAiU97E9MRY3mMeWw-2-additionalContext.txt` | 13.427 B | 2026-08-25 08:08 |
| `cb79e807-7410-4697-a11f-df2dc66ff7dc\subagents\agent-a840f8c3705c5ab46.jsonl` | 144.049 B | 2026-08-24 11:02 |
| `cb79e807-7410-4697-a11f-df2dc66ff7dc\subagents\agent-a840f8c3705c5ab46.meta.json` | 130 B | 2026-08-24 11:00 |
| `d13ec7a0-bc3b-4ce0-86b0-ee665fd03e0e\subagents\agent-a356cb256fca612dd.jsonl` | 132.579 B | 2026-08-26 11:12 |
| `d13ec7a0-bc3b-4ce0-86b0-ee665fd03e0e\subagents\agent-a356cb256fca612dd.meta.json` | 138 B | 2026-08-26 11:10 |
| `d14d7711-2d79-4e10-a509-0c78e63ad67b\subagents\agent-a0876cc26969a548a.jsonl` | 160.672 B | 2026-08-23 21:48 |
| `d14d7711-2d79-4e10-a509-0c78e63ad67b\subagents\agent-a0876cc26969a548a.meta.json` | 129 B | 2026-08-23 21:46 |
| `d58b040d-995f-488d-820d-5e245b553384\subagents\agent-afab925198f37f0e7.jsonl` | 187.540 B | 2026-08-24 07:34 |
| `d58b040d-995f-488d-820d-5e245b553384\subagents\agent-afab925198f37f0e7.meta.json` | 152 B | 2026-08-24 07:31 |
| `d7fd03bd-4dd5-4f73-a414-8845fa1c044b\subagents\agent-a4f02706b7210cf05.jsonl` | 151.910 B | 2026-08-25 14:47 |
| `d7fd03bd-4dd5-4f73-a414-8845fa1c044b\subagents\agent-a4f02706b7210cf05.meta.json` | 129 B | 2026-08-25 14:46 |
| `d7fd03bd-4dd5-4f73-a414-8845fa1c044b\tool-results\b4kvckulr.txt` | 44.301 B | 2026-08-25 14:42 |
| `d9604faa-5c12-4111-8767-e2578d11e051\subagents\agent-a33d67a879c27409b.jsonl` | 114.129 B | 2026-08-26 06:58 |
| `d9604faa-5c12-4111-8767-e2578d11e051\subagents\agent-a33d67a879c27409b.meta.json` | 151 B | 2026-08-26 06:57 |
| `d9604faa-5c12-4111-8767-e2578d11e051\tool-results\bycckrc3f.txt` | 39.646 B | 2026-08-25 16:32 |
| `dad47f57-87a4-4438-98a0-b1dbe887516a\subagents\agent-a8517e5ef055c2d0a.jsonl` | 113.744 B | 2026-08-25 14:35 |
| `dad47f57-87a4-4438-98a0-b1dbe887516a\subagents\agent-a8517e5ef055c2d0a.meta.json` | 141 B | 2026-08-25 14:33 |
| `dcbcb145-e5a1-49a7-bbe0-c28c946737be\subagents\agent-a9bc50a1f9060e1ed.jsonl` | 119.655 B | 2026-08-25 10:07 |
| `dcbcb145-e5a1-49a7-bbe0-c28c946737be\subagents\agent-a9bc50a1f9060e1ed.meta.json` | 134 B | 2026-08-25 10:06 |
| `dd663b10-dee1-4a0a-afdc-ef783ba90bb1\subagents\agent-a0cf5fd16fe909b19.jsonl` | 204.020 B | 2026-08-25 16:17 |
| `dd663b10-dee1-4a0a-afdc-ef783ba90bb1\subagents\agent-a0cf5fd16fe909b19.meta.json` | 157 B | 2026-08-25 16:14 |
| `dd663b10-dee1-4a0a-afdc-ef783ba90bb1\tool-results\bgru3suhz.txt` | 56.054 B | 2026-08-25 16:05 |
| `e3d7a1ce-8313-4616-9e1e-3ca5696797ec\subagents\agent-a480171b75a2b5fa4.jsonl` | 118.369 B | 2026-08-24 07:55 |
| `e3d7a1ce-8313-4616-9e1e-3ca5696797ec\subagents\agent-a480171b75a2b5fa4.meta.json` | 129 B | 2026-08-24 07:54 |
| `eac3dd1f-8d54-4925-85a0-ca39954ed2d2\tool-results\hook-toolu_012fMc8xorDgJ44Y7w8C97jf-2-additionalContext.txt` | 10.195 B | 2026-08-24 10:54 |
| `ee16515e-6e4a-472a-b378-8ecdbfb2c7e7\subagents\agent-a82bbc5307fa3bab9.jsonl` | 162.827 B | 2026-08-26 10:58 |
| `ee16515e-6e4a-472a-b378-8ecdbfb2c7e7\subagents\agent-a82bbc5307fa3bab9.meta.json` | 132 B | 2026-08-26 10:56 |
| `f5934f68-c27e-4977-8e4c-a403dc4a40c0\subagents\agent-a1574f8704ed1da9a.jsonl` | 55.938 B | 2026-08-24 11:38 |
| `f5934f68-c27e-4977-8e4c-a403dc4a40c0\subagents\agent-a1574f8704ed1da9a.meta.json` | 147 B | 2026-08-24 11:37 |
| `f5934f68-c27e-4977-8e4c-a403dc4a40c0\subagents\agent-a19441f746e73fccb.jsonl` | 62.777 B | 2026-08-24 11:38 |
| `f5934f68-c27e-4977-8e4c-a403dc4a40c0\subagents\agent-a19441f746e73fccb.meta.json` | 147 B | 2026-08-24 11:37 |
| `f5934f68-c27e-4977-8e4c-a403dc4a40c0\subagents\agent-aedb705170fd634f9.jsonl` | 85.541 B | 2026-08-24 11:38 |
| `f5934f68-c27e-4977-8e4c-a403dc4a40c0\subagents\agent-aedb705170fd634f9.meta.json` | 149 B | 2026-08-24 11:37 |
| `ff14d81a-a2a5-4e4c-9c44-6d63e86deea4\subagents\agent-a4eb58a99c3637e2a.jsonl` | 122.286 B | 2026-08-25 13:24 |
| `ff14d81a-a2a5-4e4c-9c44-6d63e86deea4\subagents\agent-a4eb58a99c3637e2a.meta.json` | 139 B | 2026-08-25 13:23 |

### `d--DEV-Projects-IDSD`

9 arquivo(s) - 3.29 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `14e072b0-b4a3-4856-a98c-c20ee6e0d535.jsonl` | 282.989 B | 2026-08-31 13:10 |
| `5ea1f1da-c9e2-4b5b-bbca-ea61a7f827e5.jsonl` | 451.910 B | 2026-08-31 12:15 |
| `6157e12f-dd93-44ea-8c76-4a77a8106a02.jsonl` | 1.185.591 B | 2026-09-01 16:18 |
| `823296ff-25ca-47f1-9b02-cc4a686f04af.jsonl` | 872.392 B | 2026-09-01 10:25 |
| `88a96560-84f0-4536-84b3-6bab1d1efa26.jsonl` | 29.084 B | 2026-09-02 07:53 |
| `5ea1f1da-c9e2-4b5b-bbca-ea61a7f827e5\subagents\agent-a6030243bb9b9d9c6.jsonl` | 498.253 B | 2026-08-31 11:59 |
| `5ea1f1da-c9e2-4b5b-bbca-ea61a7f827e5\subagents\agent-a6030243bb9b9d9c6.meta.json` | 136 B | 2026-08-31 11:38 |
| `823296ff-25ca-47f1-9b02-cc4a686f04af\tool-results\bh92n81u9.txt` | 40.475 B | 2026-09-01 09:52 |
| `823296ff-25ca-47f1-9b02-cc4a686f04af\tool-results\bs0ixeyrw.txt` | 88.698 B | 2026-09-01 09:57 |

### `d--DEV-Projects-IDSD-systems-agy-gemini`

4 arquivo(s) - 6.60 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `2bf7a030-00e9-49e9-a9b3-46b22e7b0550.jsonl` | 26.102 B | 2026-09-01 17:07 |
| `c8b093e0-0556-43fe-b828-c442db772bbe.jsonl` | 6.698.874 B | 2026-09-04 09:15 |
| `c8b093e0-0556-43fe-b828-c442db772bbe\auto-mode-classifier-error.txt` | 183.098 B | 2026-09-04 07:46 |
| `c8b093e0-0556-43fe-b828-c442db772bbe\tool-results\hook-toolu_vrtx_01X351A6zghaYi2QaLk2gnVT-2-additionalContext.txt` | 12.949 B | 2026-09-03 07:15 |

### `d--DEV-Projects-SDLC`

160 arquivo(s) - 28.73 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c.jsonl` | 1.260.843 B | 2026-08-22 22:46 |
| `0c4b2ad5-4047-467b-beb4-8ca1ad37336d.jsonl` | 559.884 B | 2026-08-19 11:50 |
| `0f6f3d7f-528e-4384-aef0-1fdc497b3fec.jsonl` | 394.284 B | 2026-08-19 11:50 |
| `1c5569b1-3f13-4dd8-9de6-5cc071868a55.jsonl` | 421.657 B | 2026-08-19 19:13 |
| `2b627fd1-a089-4f92-90e8-3b464019d1d8.jsonl` | 167.275 B | 2026-08-20 18:13 |
| `2ef960e5-41da-47d0-984d-92b89a38abfc.jsonl` | 637.638 B | 2026-08-19 19:13 |
| `374b6101-a1d0-4761-9ef3-2f855a075553.jsonl` | 797.258 B | 2026-08-19 19:13 |
| `39b4fdf5-4b6a-417f-9191-9b49ea6fc102.jsonl` | 191.791 B | 2026-08-21 16:55 |
| `4eb26544-fa4c-415e-8c65-7f674318de2e.jsonl` | 1.635.319 B | 2026-08-19 19:13 |
| `54afeebe-f38b-4850-9461-92d3e533a93d.jsonl` | 3.225.237 B | 2026-08-19 11:50 |
| `5c2ff8d8-ea7c-4b5a-b7ec-7b2a1525b73f.jsonl` | 340.188 B | 2026-08-20 18:03 |
| `6df341e5-bfa8-46bd-ac67-4b45566396bd.jsonl` | 635.851 B | 2026-08-19 19:13 |
| `70a1180e-a89a-4b3a-8104-8f8a46ad2598.jsonl` | 389.998 B | 2026-08-20 18:03 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9.jsonl` | 1.261.062 B | 2026-08-20 18:03 |
| `72553294-ac03-4aeb-9a5c-78c7776fb044.jsonl` | 711.866 B | 2026-08-19 18:55 |
| `7704ccd9-682e-4afa-a340-15b6f19d098c.jsonl` | 489.864 B | 2026-08-19 19:13 |
| `773686b6-c7fa-420a-bb53-bb4e900dc1b6.jsonl` | 410.795 B | 2026-08-20 18:03 |
| `77ba97b3-4fe8-48c5-9cb0-f0cc93a4daf3.jsonl` | 416.831 B | 2026-08-19 19:13 |
| `7b228fff-75b7-43ab-af64-20cbb2ce9f1c.jsonl` | 403.519 B | 2026-08-21 09:08 |
| `80c3bbeb-4220-4892-bc7c-88a379426664.jsonl` | 348.652 B | 2026-08-19 19:13 |
| `87d700fe-3729-4649-9938-13ddcffcb974.jsonl` | 1.782.610 B | 2026-08-22 23:41 |
| `8b2ea9f4-d270-4f72-8366-a030078c94c7.jsonl` | 967.393 B | 2026-08-19 19:13 |
| `8ea436f3-bce9-499e-a4d8-bdd5ae8682f3.jsonl` | 177.213 B | 2026-08-20 18:03 |
| `9a245d09-58db-4e29-8ade-ed2e033b5427.jsonl` | 279.170 B | 2026-08-19 17:37 |
| `a073f7b6-a3cc-4eb6-8b9f-ef8dec16fda3.jsonl` | 94.630 B | 2026-08-20 18:03 |
| `c01949c0-bf8f-4614-a634-f5ce40cc4acb.jsonl` | 154.186 B | 2026-08-19 11:50 |
| `c9a90de6-f4c7-48fd-b687-fdbb6d088e29.jsonl` | 365.224 B | 2026-08-20 14:34 |
| `ce81a9a9-1265-49d0-aa40-efa90e30f948.jsonl` | 925.175 B | 2026-08-20 18:07 |
| `e6096d4c-4bf5-41bb-8c99-2045ba6c7dd0.jsonl` | 424.357 B | 2026-08-20 18:18 |
| `f1546647-6b5a-4c10-8714-d26d1fe2f5eb.jsonl` | 811.671 B | 2026-08-20 18:18 |
| `f7cf565d-d72c-46e4-b436-5d056dee7efe.jsonl` | 557.347 B | 2026-08-19 19:13 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-a2d3155d5fdee35f9.jsonl` | 538.777 B | 2026-08-22 19:20 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-a2d3155d5fdee35f9.meta.json` | 144 B | 2026-08-22 19:07 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-a41e00412ee154ce0.jsonl` | 407.433 B | 2026-08-22 22:05 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-a41e00412ee154ce0.meta.json` | 145 B | 2026-08-22 21:52 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-a7075eff78d7b89db.jsonl` | 380.978 B | 2026-08-22 20:13 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-a7075eff78d7b89db.meta.json` | 147 B | 2026-08-22 20:05 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-a83d9275d940b2f07.jsonl` | 495.669 B | 2026-08-22 18:38 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-a83d9275d940b2f07.meta.json` | 143 B | 2026-08-22 18:27 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-ad993c00e1a920691.jsonl` | 529.070 B | 2026-08-22 18:55 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-ad993c00e1a920691.meta.json` | 143 B | 2026-08-22 18:44 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-add1728a63c196677.jsonl` | 273.835 B | 2026-08-22 20:36 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-add1728a63c196677.meta.json` | 153 B | 2026-08-22 20:29 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-af19b34fe8900a02b.jsonl` | 636.364 B | 2026-08-22 22:42 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\subagents\agent-af19b34fe8900a02b.meta.json` | 145 B | 2026-08-22 22:19 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\tool-results\b2cdca0xv.txt` | 75.329 B | 2026-08-22 22:38 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\tool-results\hook-toolu_011Sz47WvDGUCVtNEEfeCHBu-2-additionalContext.txt` | 105.163 B | 2026-08-22 22:07 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\tool-results\hook-toolu_012uxmF1fMTLFJtSqCEQrtXv-2-additionalContext.txt` | 105.163 B | 2026-08-22 19:21 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\tool-results\hook-toolu_01ASfBWU7iqXowms3ZvYAiDp-2-additionalContext.txt` | 105.163 B | 2026-08-22 20:38 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\tool-results\hook-toolu_01DMKKFHQsZeB8o93VJUkG5e-2-additionalContext.txt` | 105.163 B | 2026-08-22 20:15 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\tool-results\hook-toolu_01EbPMaxEGoKVrGodBBrVr6J-2-additionalContext.txt` | 105.163 B | 2026-08-22 18:39 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\tool-results\hook-toolu_01FLJJFsQe7YUZuQrcd5LLEy-2-additionalContext.txt` | 105.163 B | 2026-08-22 22:45 |
| `01b855d0-4395-4e2b-8b86-de228a3e8f3c\tool-results\hook-toolu_01MQzNSf7hzofu1giiLTNbFG-2-additionalContext.txt` | 105.163 B | 2026-08-22 18:57 |
| `0c4b2ad5-4047-467b-beb4-8ca1ad37336d\subagents\agent-ae37565a535427a8c.jsonl` | 143.108 B | 2026-08-19 11:17 |
| `0c4b2ad5-4047-467b-beb4-8ca1ad37336d\subagents\agent-ae37565a535427a8c.meta.json` | 127 B | 2026-08-19 11:15 |
| `0c4b2ad5-4047-467b-beb4-8ca1ad37336d\tool-results\hook-toolu_01P7njkAik4xLkb4R5nd7Hw3-2-additionalContext.txt` | 12.713 B | 2026-08-19 11:19 |
| `0f6f3d7f-528e-4384-aef0-1fdc497b3fec\tool-results\hook-toolu_01MTcLscEzxV1kTeqbbpFPKg-2-additionalContext.txt` | 12.713 B | 2026-08-19 11:04 |
| `1c5569b1-3f13-4dd8-9de6-5cc071868a55\subagents\agent-aae37009c2130bb70.jsonl` | 76.109 B | 2026-08-19 13:01 |
| `1c5569b1-3f13-4dd8-9de6-5cc071868a55\subagents\agent-aae37009c2130bb70.meta.json` | 115 B | 2026-08-19 12:58 |
| `1c5569b1-3f13-4dd8-9de6-5cc071868a55\tool-results\hook-toolu_01W6AMXgv4Cx6dddtXg3biKQ-2-additionalContext.txt` | 12.713 B | 2026-08-19 13:02 |
| `2ef960e5-41da-47d0-984d-92b89a38abfc\subagents\agent-a3230591ccb7d5b9b.jsonl` | 106.273 B | 2026-08-19 15:55 |
| `2ef960e5-41da-47d0-984d-92b89a38abfc\subagents\agent-a3230591ccb7d5b9b.meta.json` | 118 B | 2026-08-19 15:51 |
| `2ef960e5-41da-47d0-984d-92b89a38abfc\tool-results\hook-toolu_0158wA1cgitFVQGtHLtAtd9G-2-additionalContext.txt` | 12.713 B | 2026-08-19 15:58 |
| `374b6101-a1d0-4761-9ef3-2f855a075553\subagents\agent-afaa3560287e1d5b7.jsonl` | 111.810 B | 2026-08-19 13:56 |
| `374b6101-a1d0-4761-9ef3-2f855a075553\subagents\agent-afaa3560287e1d5b7.meta.json` | 123 B | 2026-08-19 13:54 |
| `374b6101-a1d0-4761-9ef3-2f855a075553\tool-results\hook-toolu_01FieN7NefQQVhX4oZQ7mrne-2-additionalContext.txt` | 12.713 B | 2026-08-19 14:01 |
| `374b6101-a1d0-4761-9ef3-2f855a075553\tool-results\hook-toolu_01KCDjuTpqsBmQVNHYroXGBd-2-additionalContext.txt` | 12.713 B | 2026-08-19 14:18 |
| `4eb26544-fa4c-415e-8c65-7f674318de2e\subagents\agent-a07cdd56a7eec48c3.jsonl` | 98.312 B | 2026-08-19 16:15 |
| `4eb26544-fa4c-415e-8c65-7f674318de2e\subagents\agent-a07cdd56a7eec48c3.meta.json` | 134 B | 2026-08-19 16:13 |
| `4eb26544-fa4c-415e-8c65-7f674318de2e\subagents\agent-a4bad23954a29fd47.jsonl` | 81.562 B | 2026-08-19 15:35 |
| `4eb26544-fa4c-415e-8c65-7f674318de2e\subagents\agent-a4bad23954a29fd47.meta.json` | 125 B | 2026-08-19 15:28 |
| `4eb26544-fa4c-415e-8c65-7f674318de2e\subagents\agent-a5335679ec0bab0fa.jsonl` | 214.766 B | 2026-08-19 16:47 |
| `4eb26544-fa4c-415e-8c65-7f674318de2e\subagents\agent-a5335679ec0bab0fa.meta.json` | 134 B | 2026-08-19 16:43 |
| `4eb26544-fa4c-415e-8c65-7f674318de2e\tool-results\hook-toolu_01NTshhQZ98jd2RLwKgxUUKu-2-additionalContext.txt` | 12.713 B | 2026-08-19 15:35 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-a53eb4a47ba4be8ba.jsonl` | 218.737 B | 2026-08-18 17:49 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-a53eb4a47ba4be8ba.meta.json` | 125 B | 2026-08-18 17:40 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-a61a7d1f9739036a7.jsonl` | 196.958 B | 2026-08-19 10:15 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-a61a7d1f9739036a7.meta.json` | 126 B | 2026-08-19 10:14 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-a76274fe521487e6a.jsonl` | 98.777 B | 2026-08-19 08:28 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-a76274fe521487e6a.meta.json` | 116 B | 2026-08-19 08:27 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-a9ad1cc93da2597d0.jsonl` | 193.299 B | 2026-08-19 10:15 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-a9ad1cc93da2597d0.meta.json` | 135 B | 2026-08-19 10:14 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-ac7801afc77c64ebb.jsonl` | 99.040 B | 2026-08-19 08:28 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-ac7801afc77c64ebb.meta.json` | 118 B | 2026-08-19 08:27 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-acb88732266cd2837.jsonl` | 93.485 B | 2026-08-19 08:28 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-acb88732266cd2837.meta.json` | 117 B | 2026-08-19 08:27 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-acc64948ad61db94d.jsonl` | 101.496 B | 2026-08-19 08:28 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-acc64948ad61db94d.meta.json` | 123 B | 2026-08-19 08:27 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-af76f6f483cab696f.jsonl` | 119.178 B | 2026-08-19 08:28 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\subagents\agent-af76f6f483cab696f.meta.json` | 125 B | 2026-08-19 08:27 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\tool-results\hook-toolu_011dLZj1VzLHfbpiLEJQxJBc-2-additionalContext.txt` | 16.225 B | 2026-08-19 08:47 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\tool-results\hook-toolu_016muEimsHu52MiWWqtdQ4AD-2-additionalContext.txt` | 16.606 B | 2026-08-18 17:18 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\tool-results\hook-toolu_01C89aCsmQuShAQbqR6BAhUA-2-additionalContext.txt` | 11.898 B | 2026-08-19 08:48 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\tool-results\hook-toolu_01DYcjKMFv7pBoSCBTPkwNfj-2-additionalContext.txt` | 129.054 B | 2026-08-19 10:20 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\tool-results\hook-toolu_01Mhd5pWjJLZkuA7zdgg3Suw-2-additionalContext.txt` | 16.496 B | 2026-08-19 08:47 |
| `54afeebe-f38b-4850-9461-92d3e533a93d\tool-results\hook-toolu_01RPMsegcd6eg43A7ke8XoaS-2-additionalContext.txt` | 128.498 B | 2026-08-19 10:17 |
| `5c2ff8d8-ea7c-4b5a-b7ec-7b2a1525b73f\subagents\agent-a1760662f4ea4338b.jsonl` | 103.722 B | 2026-08-20 14:48 |
| `5c2ff8d8-ea7c-4b5a-b7ec-7b2a1525b73f\subagents\agent-a1760662f4ea4338b.meta.json` | 115 B | 2026-08-20 14:43 |
| `5c2ff8d8-ea7c-4b5a-b7ec-7b2a1525b73f\tool-results\hook-toolu_01SZLgkKStNgh7YgsaVtDjuS-2-additionalContext.txt` | 12.713 B | 2026-08-20 14:48 |
| `6df341e5-bfa8-46bd-ac67-4b45566396bd\subagents\agent-af148bffeffc01781.jsonl` | 65.397 B | 2026-08-19 17:26 |
| `6df341e5-bfa8-46bd-ac67-4b45566396bd\subagents\agent-af148bffeffc01781.meta.json` | 129 B | 2026-08-19 17:25 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\subagents\agent-a1f27d9abe7d5a1d0.jsonl` | 38.448 B | 2026-08-20 15:06 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\subagents\agent-a1f27d9abe7d5a1d0.meta.json` | 115 B | 2026-08-20 15:05 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\subagents\agent-aa871bf9ea5da4d3c.jsonl` | 66.354 B | 2026-08-20 15:17 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\subagents\agent-aa871bf9ea5da4d3c.meta.json` | 115 B | 2026-08-20 15:16 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\tool-results\hook-toolu_0111Wxa4Zf5x1gHsyyFoCRNH-2-additionalContext.txt` | 40.075 B | 2026-08-20 15:26 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\tool-results\hook-toolu_011x6aevCzpfx6ey2thqscuY-2-additionalContext.txt` | 130.267 B | 2026-08-20 15:18 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\tool-results\hook-toolu_017JYF2vv5YwtgXMKt3Regzj-2-additionalContext.txt` | 49.112 B | 2026-08-20 15:26 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\tool-results\hook-toolu_019xLFVdTo5LpGPR6M6e9jpX-2-additionalContext.txt` | 130.267 B | 2026-08-20 15:07 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\tool-results\hook-toolu_01D1HdR6GaGVWv9FxyjdwCmv-2-additionalContext.txt` | 12.713 B | 2026-08-20 15:07 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\tool-results\hook-toolu_01GmdLENCsJNZrxaLdoh7Qrj-2-additionalContext.txt` | 12.713 B | 2026-08-20 15:22 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\tool-results\hook-toolu_01QzXotiEbFWK6u1a6c229hD-2-additionalContext.txt` | 12.713 B | 2026-08-20 15:31 |
| `722db6b7-dc15-4201-a7ce-62161f684fd9\tool-results\hook-toolu_01R2ghEN4QCXWqyBjYmTJa1T-2-additionalContext.txt` | 12.713 B | 2026-08-20 15:18 |
| `72553294-ac03-4aeb-9a5c-78c7776fb044\subagents\agent-a2a04fe00c8a6fed5.jsonl` | 97.498 B | 2026-08-19 18:53 |
| `72553294-ac03-4aeb-9a5c-78c7776fb044\subagents\agent-a2a04fe00c8a6fed5.meta.json` | 115 B | 2026-08-19 18:53 |
| `72553294-ac03-4aeb-9a5c-78c7776fb044\tool-results\hook-toolu_016i8qpSq97KQw4JuUzkBWLQ-2-additionalContext.txt` | 12.713 B | 2026-08-19 18:54 |
| `7704ccd9-682e-4afa-a340-15b6f19d098c\subagents\agent-a2b9fb89f18531a0f.jsonl` | 65.821 B | 2026-08-19 13:13 |
| `7704ccd9-682e-4afa-a340-15b6f19d098c\subagents\agent-a2b9fb89f18531a0f.meta.json` | 133 B | 2026-08-19 13:12 |
| `7704ccd9-682e-4afa-a340-15b6f19d098c\tool-results\hook-toolu_01S7u47sLNM2Gbe8r483R7Nw-2-additionalContext.txt` | 12.713 B | 2026-08-19 13:34 |
| `773686b6-c7fa-420a-bb53-bb4e900dc1b6\subagents\agent-adfc3a8f962d4f878.jsonl` | 45.286 B | 2026-08-20 14:57 |
| `773686b6-c7fa-420a-bb53-bb4e900dc1b6\subagents\agent-adfc3a8f962d4f878.meta.json` | 115 B | 2026-08-20 14:56 |
| `773686b6-c7fa-420a-bb53-bb4e900dc1b6\tool-results\hook-toolu_011X9LWs4VNCCBsqEY1VKf8h-2-additionalContext.txt` | 12.713 B | 2026-08-20 14:58 |
| `773686b6-c7fa-420a-bb53-bb4e900dc1b6\tool-results\hook-toolu_01Am6EaXdUm6g7wQdbH6cr2f-2-additionalContext.txt` | 130.267 B | 2026-08-20 14:58 |
| `77ba97b3-4fe8-48c5-9cb0-f0cc93a4daf3\subagents\agent-a4b75ce64a0a2f75e.jsonl` | 118.357 B | 2026-08-19 14:37 |
| `77ba97b3-4fe8-48c5-9cb0-f0cc93a4daf3\subagents\agent-a4b75ce64a0a2f75e.meta.json` | 115 B | 2026-08-19 14:34 |
| `77ba97b3-4fe8-48c5-9cb0-f0cc93a4daf3\tool-results\hook-toolu_01SKbVrmWTvvtZbjQKZgyaqi-2-additionalContext.txt` | 12.713 B | 2026-08-19 14:38 |
| `7b228fff-75b7-43ab-af64-20cbb2ce9f1c\tool-results\bpsq9hwum.txt` | 38.972 B | 2026-08-21 08:10 |
| `80c3bbeb-4220-4892-bc7c-88a379426664\tool-results\hook-toolu_01NohHECqcVBAPrAzdMLvnKY-2-additionalContext.txt` | 12.713 B | 2026-08-19 11:46 |
| `87d700fe-3729-4649-9938-13ddcffcb974\tool-results\hook-toolu_015p7pFMBPpvomD6dqd64wrp-2-additionalContext.txt` | 10.147 B | 2026-08-22 23:07 |
| `87d700fe-3729-4649-9938-13ddcffcb974\tool-results\hook-toolu_016uQVZFGXNPijgATXNTV2ET-2-additionalContext.txt` | 10.147 B | 2026-08-22 23:39 |
| `87d700fe-3729-4649-9938-13ddcffcb974\tool-results\hook-toolu_01HTSmYwrGD9amJcdVVJ77yz-2-additionalContext.txt` | 105.163 B | 2026-08-22 23:07 |
| `87d700fe-3729-4649-9938-13ddcffcb974\tool-results\hook-toolu_01Ms2RSbhUvNa5hQKwS1EpVr-2-additionalContext.txt` | 10.147 B | 2026-08-22 23:27 |
| `87d700fe-3729-4649-9938-13ddcffcb974\tool-results\hook-toolu_01SrgwdZEBzg8ohL4yYPKZUV-2-additionalContext.txt` | 105.163 B | 2026-08-22 23:27 |
| `87d700fe-3729-4649-9938-13ddcffcb974\tool-results\hook-toolu_01X2bR94riwfTWigDyogREQi-2-additionalContext.txt` | 105.163 B | 2026-08-22 23:39 |
| `8b2ea9f4-d270-4f72-8366-a030078c94c7\subagents\agent-aef01365b74d15c16.jsonl` | 135.198 B | 2026-08-19 18:38 |
| `8b2ea9f4-d270-4f72-8366-a030078c94c7\subagents\agent-aef01365b74d15c16.meta.json` | 123 B | 2026-08-19 18:35 |
| `8b2ea9f4-d270-4f72-8366-a030078c94c7\tool-results\hook-toolu_01Q3zBXL8TEMfovenbBdPbqk-2-additionalContext.txt` | 12.713 B | 2026-08-19 18:39 |
| `9a245d09-58db-4e29-8ade-ed2e033b5427\subagents\agent-aba507060c7139b61.jsonl` | 43.614 B | 2026-08-19 17:36 |
| `9a245d09-58db-4e29-8ade-ed2e033b5427\subagents\agent-aba507060c7139b61.meta.json` | 115 B | 2026-08-19 17:36 |
| `9a245d09-58db-4e29-8ade-ed2e033b5427\tool-results\hook-toolu_01GRtYoahJVE94M36xTjnfsv-2-additionalContext.txt` | 12.713 B | 2026-08-19 17:36 |
| `c01949c0-bf8f-4614-a634-f5ce40cc4acb\tool-results\hook-toolu_01En542LYzkqXYowYhM2Ea3f-2-additionalContext.txt` | 12.713 B | 2026-08-19 11:28 |
| `c9a90de6-f4c7-48fd-b687-fdbb6d088e29\subagents\agent-a368c075767a40a65.jsonl` | 68.770 B | 2026-08-20 14:33 |
| `c9a90de6-f4c7-48fd-b687-fdbb6d088e29\subagents\agent-a368c075767a40a65.meta.json` | 115 B | 2026-08-20 14:29 |
| `c9a90de6-f4c7-48fd-b687-fdbb6d088e29\tool-results\hook-toolu_01WGk6xACp6gjY2cW6yAk6Ya-2-additionalContext.txt` | 12.713 B | 2026-08-20 14:33 |
| `ce81a9a9-1265-49d0-aa40-efa90e30f948\subagents\agent-a0e1e8c263c180ec3.jsonl` | 100.629 B | 2026-08-20 10:48 |
| `ce81a9a9-1265-49d0-aa40-efa90e30f948\subagents\agent-a0e1e8c263c180ec3.meta.json` | 115 B | 2026-08-20 10:45 |
| `ce81a9a9-1265-49d0-aa40-efa90e30f948\subagents\agent-a590aca04f252bdf4.jsonl` | 60.650 B | 2026-08-20 13:52 |
| `ce81a9a9-1265-49d0-aa40-efa90e30f948\subagents\agent-a590aca04f252bdf4.meta.json` | 115 B | 2026-08-20 13:50 |
| `ce81a9a9-1265-49d0-aa40-efa90e30f948\tool-results\hook-toolu_016ueNdQfQrAsqAFmetDX8et-2-additionalContext.txt` | 12.713 B | 2026-08-20 10:49 |
| `ce81a9a9-1265-49d0-aa40-efa90e30f948\tool-results\hook-toolu_01Pzy43cZASCTwqMYQ1gtteS-2-additionalContext.txt` | 12.713 B | 2026-08-20 13:53 |
| `e6096d4c-4bf5-41bb-8c99-2045ba6c7dd0\subagents\agent-a02eadad3d8be6e38.jsonl` | 101.826 B | 2026-08-20 11:24 |
| `e6096d4c-4bf5-41bb-8c99-2045ba6c7dd0\subagents\agent-a02eadad3d8be6e38.meta.json` | 115 B | 2026-08-20 11:18 |
| `e6096d4c-4bf5-41bb-8c99-2045ba6c7dd0\tool-results\hook-toolu_0154fPZrboxfR482QaMNy7we-2-additionalContext.txt` | 14.387 B | 2026-08-20 11:24 |
| `e6096d4c-4bf5-41bb-8c99-2045ba6c7dd0\tool-results\hook-toolu_01LDqT4XNU8H5mMYvFbQvetG-2-additionalContext.txt` | 12.713 B | 2026-08-20 11:24 |
| `f1546647-6b5a-4c10-8714-d26d1fe2f5eb\subagents\agent-aa8b7726c8982d161.jsonl` | 154.605 B | 2026-08-20 09:44 |
| `f1546647-6b5a-4c10-8714-d26d1fe2f5eb\subagents\agent-aa8b7726c8982d161.meta.json` | 115 B | 2026-08-20 09:40 |
| `f1546647-6b5a-4c10-8714-d26d1fe2f5eb\tool-results\hook-toolu_01L46NMWYACzE1fMzauJgEpR-2-additionalContext.txt` | 12.713 B | 2026-08-20 09:50 |
| `f7cf565d-d72c-46e4-b436-5d056dee7efe\subagents\agent-a0cd0e57a57155b83.jsonl` | 59.059 B | 2026-08-19 14:55 |
| `f7cf565d-d72c-46e4-b436-5d056dee7efe\subagents\agent-a0cd0e57a57155b83.meta.json` | 118 B | 2026-08-19 14:54 |
| `f7cf565d-d72c-46e4-b436-5d056dee7efe\tool-results\hook-toolu_0168wcJBYTLcvmcshBbbGX83-2-additionalContext.txt` | 12.713 B | 2026-08-19 14:57 |

### `d--DEV-Projects-SPDD-puro`

0 arquivo(s) - 0.00 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| _(vazia)_ |  |  |

### `d--DEV-Projects-SPDD-puro-comparando-idsd`

4 arquivo(s) - 0.78 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `45c10f59-ee22-4bf7-a884-7331a1054fcb.jsonl` | 221.021 B | 2026-09-04 11:37 |
| `a6165e90-d1e7-492b-9aa8-a5c3de07cc64.jsonl` | 337.601 B | 2026-09-04 10:24 |
| `45c10f59-ee22-4bf7-a884-7331a1054fcb\auto-mode-classifier-error.txt` | 131.417 B | 2026-09-04 11:29 |
| `a6165e90-d1e7-492b-9aa8-a5c3de07cc64\auto-mode-classifier-error.txt` | 127.598 B | 2026-09-04 09:20 |

### `d--DEV-Projects-SPDD-puro-poc`

6 arquivo(s) - 7.54 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `68ab90e7-2d77-4918-913d-ae4d855bb09a.jsonl` | 520.710 B | 2026-09-04 10:44 |
| `84ca5e75-c4be-480e-bd3f-a0b668722163.jsonl` | 5.232.006 B | 2026-09-04 10:39 |
| `97307712-b5cd-4a4e-b8e9-d08f07d3cd5c.jsonl` | 1.838.518 B | 2026-09-04 10:32 |
| `68ab90e7-2d77-4918-913d-ae4d855bb09a\auto-mode-classifier-error.txt` | 127.942 B | 2026-09-03 14:15 |
| `97307712-b5cd-4a4e-b8e9-d08f07d3cd5c\auto-mode-classifier-error.txt` | 180.740 B | 2026-09-04 10:11 |
| `97307712-b5cd-4a4e-b8e9-d08f07d3cd5c\tool-results\hook-toolu_vrtx_01449TzfNtmrsdJVB9mppwiP-2-additionalContext.txt` | 10.630 B | 2026-09-04 09:51 |

### `d--DEV-Projects-SPDD-puro-poc-v2`

2 arquivo(s) - 1.52 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `1285e682-08b4-4088-a8a4-efd24feda389.jsonl` | 1.466.992 B | 2026-09-04 11:31 |
| `1285e682-08b4-4088-a8a4-efd24feda389\auto-mode-classifier-error.txt` | 127.465 B | 2026-09-04 10:45 |

### `d--DEV-Projects-heroi`

1 arquivo(s) - 0.03 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `4d14566d-cfa5-4228-83fb-c6f9c100c030.jsonl` | 28.508 B | 2026-09-01 14:14 |

### `d--DEV-Projects-idsd-full-claude`

72 arquivo(s) - 22.73 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `0a2d7a56-862f-4fc6-8e0f-a13991d48701.jsonl` | 933.076 B | 2026-09-04 11:13 |
| `4f9b1f9b-0e2f-4f1c-b1bc-ddbb0256a7ef.jsonl` | 782.292 B | 2026-09-04 11:34 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5.jsonl` | 5.888.612 B | 2026-09-04 07:24 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06.jsonl` | 11.532.259 B | 2026-09-03 11:55 |
| `df193568-36df-4952-9595-e92e4dbd7524.jsonl` | 835.192 B | 2026-09-04 08:43 |
| `0a2d7a56-862f-4fc6-8e0f-a13991d48701\auto-mode-classifier-error.txt` | 173.236 B | 2026-09-04 10:14 |
| `0a2d7a56-862f-4fc6-8e0f-a13991d48701\subagents\agent-a3a078a6ee1bac3ad.jsonl` | 342.467 B | 2026-09-04 09:51 |
| `0a2d7a56-862f-4fc6-8e0f-a13991d48701\subagents\agent-a3a078a6ee1bac3ad.meta.json` | 134 B | 2026-09-04 09:40 |
| `0a2d7a56-862f-4fc6-8e0f-a13991d48701\tool-results\hook-toolu_vrtx_01CH3hQLnGGUWvMffhudV3pm-2-additionalContext.txt` | 23.607 B | 2026-09-04 09:49 |
| `0a2d7a56-862f-4fc6-8e0f-a13991d48701\tool-results\hook-toolu_vrtx_01KsSNTk1qSbTpHjZ5kmMUqe-2-additionalContext.txt` | 36.662 B | 2026-09-04 10:58 |
| `4f9b1f9b-0e2f-4f1c-b1bc-ddbb0256a7ef\tool-results\hook-toolu_vrtx_016b5TFJK3RM9eUWjMYb2Rcr-2-additionalContext.txt` | 43.739 B | 2026-09-04 11:30 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\auto-mode-classifier-error.txt` | 187.916 B | 2026-09-03 14:37 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\subagents\agent-a58d0c4e84685f37c.jsonl` | 5.183 B | 2026-09-03 13:32 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\subagents\agent-a58d0c4e84685f37c.meta.json` | 159 B | 2026-09-03 13:29 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\subagents\agent-a7f33010564ff097e.jsonl` | 258.954 B | 2026-09-03 13:39 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\subagents\agent-a7f33010564ff097e.meta.json` | 142 B | 2026-09-03 13:32 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_011NAvaBdqc2o3sS5QbawqqV-2-additionalContext.txt` | 10.717 B | 2026-09-03 13:14 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_011e1xCNc1zhTQFjW5k4vcPx-2-additionalContext.txt` | 43.739 B | 2026-09-03 17:10 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_013N2mxdXr4igZMiQriPc7Xi-2-additionalContext.txt` | 20.024 B | 2026-09-03 14:51 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_014qUcQKHk9uFTodvNXm8ksy-2-additionalContext.txt` | 11.656 B | 2026-09-03 12:07 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_016q5hbZREtKCbTFe7b7SiRg-2-additionalContext.txt` | 17.076 B | 2026-09-03 12:07 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_017qc2U2XaBxuxi9WFAx1t2c-2-additionalContext.txt` | 43.739 B | 2026-09-03 14:14 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_01GNsgozWGupYNhRS8qhFDtf-2-additionalContext.txt` | 20.024 B | 2026-09-03 12:06 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_01JUtG7dVsuNmPPkJwkornbm-2-additionalContext.txt` | 11.981 B | 2026-09-03 12:07 |
| `7bee1a17-992d-4bc3-9c55-2013938453f5\tool-results\hook-toolu_vrtx_01LZxPyd1uaJHZZ5GFp9e8s4-2-additionalContext.txt` | 23.590 B | 2026-09-03 13:38 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\auto-mode-classifier-error.txt` | 157.540 B | 2026-09-03 08:06 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-a3f829f53f2e9598d.jsonl` | 179.785 B | 2026-09-03 08:10 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-a3f829f53f2e9598d.meta.json` | 140 B | 2026-09-03 08:08 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-a78866a0d1003dde4.jsonl` | 280.926 B | 2026-09-03 11:47 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-a78866a0d1003dde4.meta.json` | 134 B | 2026-09-03 11:39 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-a84ca065a9b23a5bf.jsonl` | 583.011 B | 2026-09-02 16:23 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-a84ca065a9b23a5bf.meta.json` | 128 B | 2026-09-02 15:55 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-a9fad1468af113cc8.jsonl` | 165.548 B | 2026-09-03 08:10 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-a9fad1468af113cc8.meta.json` | 140 B | 2026-09-03 08:07 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-ad001925934832cae.jsonl` | 183.949 B | 2026-09-03 08:11 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-ad001925934832cae.meta.json` | 146 B | 2026-09-03 08:09 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-ad3195ad3b24c55cc.jsonl` | 101.716 B | 2026-09-03 08:10 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-ad3195ad3b24c55cc.meta.json` | 133 B | 2026-09-03 08:08 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-add9396ec78cdce47.jsonl` | 108.956 B | 2026-09-03 08:10 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\subagents\agent-add9396ec78cdce47.meta.json` | 135 B | 2026-09-03 08:08 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_013uEryUrpmRhfwGgLYy2Yj2-2-additionalContext.txt` | 10.999 B | 2026-09-03 06:54 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_014RTLfLwCTXRnAjYUbtLUCS-2-additionalContext.txt` | 11.809 B | 2026-09-02 15:16 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_019t9K8orZscD6fLCPW76DfZ-2-additionalContext.txt` | 10.013 B | 2026-09-03 09:43 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01AsiwqG1AzYNP8YbPSJmvgm-2-additionalContext.txt` | 13.699 B | 2026-09-02 15:34 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01BzDTgLCJ5tVXY2xj3roJBj-2-additionalContext.txt` | 30.632 B | 2026-09-03 10:41 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01Cac63kzsY3PvDpDVwe6jtQ-2-additionalContext.txt` | 14.413 B | 2026-09-03 08:21 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01DQbKuZWpL3txzorXcgrEFj-2-additionalContext.txt` | 30.632 B | 2026-09-03 11:09 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01Ec9aKa6bchxXKxqReudWyL-2-additionalContext.txt` | 30.445 B | 2026-09-03 07:52 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01FnmV6sspLEsicHp8RJtwqg-2-additionalContext.txt` | 31.487 B | 2026-09-02 15:51 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01HMxSgPHvQXdqV8YZ8w2f22-2-additionalContext.txt` | 31.487 B | 2026-09-02 16:24 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01Jne4EeZXneLjZP18bFEKrC-2-additionalContext.txt` | 21.726 B | 2026-09-03 11:46 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01KUHsXotsfezRndgRSjMZPg-2-additionalContext.txt` | 15.491 B | 2026-09-03 07:17 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01KoRMtEMFWxkjK2G1dFMcE6-2-additionalContext.txt` | 10.999 B | 2026-09-03 07:11 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01L6Fjb46XfZR9dATxnsseBS-2-additionalContext.txt` | 15.771 B | 2026-09-02 16:26 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01MJxLAGE9T99R5wDSbVc2Qc-2-additionalContext.txt` | 30.921 B | 2026-09-02 15:38 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01NZV8jFbwPxhYYPAAkXyGn2-2-additionalContext.txt` | 18.504 B | 2026-09-03 08:22 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01QGtcVcVqSFjCrLWtoEzKbc-2-additionalContext.txt` | 26.752 B | 2026-09-02 15:56 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01QeuwpF6soZafPSfmYc2exm-2-additionalContext.txt` | 15.491 B | 2026-09-02 14:48 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01RGLKTG6ottzCZFCyPJfKfB-2-additionalContext.txt` | 18.504 B | 2026-09-02 13:09 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01RJmHpaAohFkbAizBc1cyWT-2-additionalContext.txt` | 10.050 B | 2026-09-02 13:21 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01RmKDb6bbnameHehqhKnSUM-2-additionalContext.txt` | 10.729 B | 2026-09-02 13:13 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01Tjp8usmqHqTdjGesZQdCms-2-additionalContext.txt` | 16.171 B | 2026-09-03 08:48 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01UU2TQzTyFSZwMTU6vxzFV3-2-additionalContext.txt` | 24.360 B | 2026-09-03 09:41 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01VJUrkTJaP5qZ215xGQwSi3-2-additionalContext.txt` | 15.491 B | 2026-09-03 07:13 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01WN7svpQJaA9DwYdbxmLV2b-2-additionalContext.txt` | 13.823 B | 2026-09-03 10:23 |
| `d2540d2c-a7c3-44e2-a283-e3b21eaaed06\tool-results\hook-toolu_vrtx_01YGBnXrasMPxNK22gTQXghk-2-additionalContext.txt` | 34.886 B | 2026-09-02 16:27 |
| `df193568-36df-4952-9595-e92e4dbd7524\subagents\agent-a90e201613c4efd56.jsonl` | 239.583 B | 2026-09-04 08:09 |
| `df193568-36df-4952-9595-e92e4dbd7524\subagents\agent-a90e201613c4efd56.meta.json` | 134 B | 2026-09-04 08:02 |
| `df193568-36df-4952-9595-e92e4dbd7524\tool-results\hook-toolu_vrtx_01SBykihjB99cZVGutMsDhGA-2-additionalContext.txt` | 23.607 B | 2026-09-04 08:08 |
| `df193568-36df-4952-9595-e92e4dbd7524\tool-results\hook-toolu_vrtx_01U5vcanpP6qZT9NS7C1LBnh-2-additionalContext.txt` | 43.739 B | 2026-09-04 07:47 |
| `memory\MEMORY.md` | 151 B | 2026-09-02 16:08 |
| `memory\salvamento-incremental-de-artefatos.md` | 1.406 B | 2026-09-02 16:07 |

### `d--DEV-Projects-vanguarda`

1 arquivo(s) - 0.17 MB

| Arquivo | Tamanho | Modificado |
|---|--:|---|
| `97a48553-beec-4a0d-9f0c-058a3ca9e276.jsonl` | 176.691 B | 2026-08-18 11:36 |

---

## 2. Sumarizacao do inventario

22 pastas de projeto - 566 arquivos - 187.84 MB no total.

| Pasta | Arquivos | Tamanho (MB) | Sessoes com uso | Sessoes no recorte |
|---|--:|--:|--:|--:|
| `d--DEV-Projects-CRUDAO` | 207 | 56.99 | 61 | 0 |
| `D--DEV-Projects-CRUDAO-experimentos-quarta-versao` | 42 | 30.25 | 18 | 0 |
| `d--DEV-Projects-SDLC` | 160 | 28.73 | 31 | 0 |
| `d--DEV-Projects-idsd-full-claude` | 72 | 22.73 | 5 | 5 |
| `d--CobraKai-SDD-meuSDD` | 11 | 13.43 | 7 | 0 |
| `D--CobraKai-SDD-meuSDD-SSPDD` | 12 | 8.33 | 12 | 0 |
| `d--DEV-Projects-SPDD-puro-poc` | 6 | 7.54 | 3 | 3 |
| `d--DEV-Projects-IDSD-systems-agy-gemini` | 4 | 6.60 | 2 | 1 |
| `d--DEV-Projects-2030-countdown` | 8 | 5.49 | 4 | 0 |
| `d--DEV-Projects-IDSD` | 9 | 3.29 | 5 | 1 |
| `D--DEV-Projects-CRUDAO-primeira-versao` | 7 | 1.82 | 5 | 0 |
| `d--DEV-Projects-SPDD-puro-poc-v2` | 2 | 1.52 | 1 | 1 |
| `d--DEV-Projects-SPDD-puro-comparando-idsd` | 4 | 0.78 | 2 | 2 |
| `d--DEV-Projects-vanguarda` | 1 | 0.17 | 1 | 0 |
| `C--Users-User` | 3 | 0.08 | 2 | 2 |
| `C--Users-User-AppData-Roaming-Claude-scratch-workspaces-8ea5fdd5-ef38-4e15-b97c-6d37cee37f2c-c07d2e3c-a6c3-4c16-a34b-b1c8a7b0af97-scratch-2026-09-01-4fb476` | 1 | 0.05 | 1 | 0 |
| `d--DEV-Projects-heroi` | 1 | 0.03 | 1 | 0 |
| `D--CobraKai-bcorp` | 5 | 0.01 | 0 | 0 |
| `D--CobraKai-SDD` | 6 | 0.00 | 0 | 0 |
| `D--DEV-Projects-spacescape-app` | 3 | 0.00 | 0 | 0 |
| `D--DEV-Projects-ultimateforce` | 2 | 0.00 | 0 | 0 |
| `d--DEV-Projects-SPDD-puro` | 0 | 0.00 | 0 | 0 |

| **TOTAL** | **566** | **187.84** | **161** | **15** |

---

## 3. Consumo por workspace (sessoes iniciadas a partir de 2026-09-02 00:00)

| Workspace (`cwd`) | Sessoes | Requisicoes | Input | Cache write | Cache read | Output | Custo est. (US$) |
|---|--:|--:|--:|--:|--:|--:|--:|
| `d:\DEV\Projects\idsd-full-claude` | 5 | 1.710 | 3.410 | 9.189.716 | 169.893.800 | 1.119.330 | 170.38 |
| `d:\DEV\Projects\IDSD\systems\agy_gemini` | 1 | 517 | 1.034 | 3.902.298 | 46.679.114 | 429.032 | 58.46 |
| `d:\DEV\Projects\SPDD_puro\poc` | 3 | 450 | 900 | 2.020.137 | 43.059.252 | 510.377 | 46.92 |
| `d:\DEV\Projects\SPDD_puro\poc_v2` | 1 | 132 | 264 | 410.070 | 13.846.431 | 163.124 | 13.57 |
| `d:\DEV\Projects\SPDD_puro\comparando_idsd` | 2 | 47 | 92 | 253.876 | 2.451.243 | 34.458 | 3.67 |
| `C:\Users\User` | 2 | 2 | 4 | 40.989 | 0 | 96 | 0.26 |
| `D:\DEV\Projects\IDSD` | 1 | 1 | 2 | 27.871 | 0 | 42 | 0.18 |
| **TOTAL** | **15** | **2.859** | **5.706** | **15.844.957** | **275.929.840** | **2.256.459** | **293.44** |

---

## 4. Detalhe por sessao

### `d:\DEV\Projects\idsd-full-claude`

5 sessao(oes) - 179.086.926 tokens de entrada - 1.119.330 de saida - US$ 170.38

| Sessao | Inicio | Fim | Req | Input | Cache write | Cache read | Output | Modelos | Custo est. |
|---|---|---|--:|--:|--:|--:|--:|---|--:|
| `d2540d2c` | 2026-09-02 14:32 | 2026-09-03 14:48 | 840 | 1.680 | 4.689.724 | 79.733.633 | 581.176 | opus-5x840 | 83.72 |
| `7bee1a17` | 2026-09-03 14:55 | 2026-09-03 20:11 | 572 | 1.142 | 2.322.109 | 59.853.615 | 385.161 | opus-5x571, <synthetic>x1 | 54.07 |
| `df193568` | 2026-09-04 10:24 | 2026-09-04 11:27 | 104 | 208 | 743.206 | 10.668.452 | 53.894 | opus-5x104 | 11.33 |
| `0a2d7a56` | 2026-09-04 11:43 | 2026-09-04 14:03 | 132 | 256 | 1.216.729 | 12.209.662 | 52.254 | opus-5x128, <synthetic>x4 | 15.02 |
| `4f9b1f9b` | 2026-09-04 14:13 | 2026-09-04 14:34 | 62 | 124 | 217.948 | 7.428.438 | 46.845 | opus-5x62 | 6.25 |

### `d:\DEV\Projects\IDSD\systems\agy_gemini`

1 sessao(oes) - 50.582.446 tokens de entrada - 429.032 de saida - US$ 58.46

| Sessao | Inicio | Fim | Req | Input | Cache write | Cache read | Output | Modelos | Custo est. |
|---|---|---|--:|--:|--:|--:|--:|---|--:|
| `c8b093e0` | 2026-09-02 11:07 | 2026-09-04 11:54 | 517 | 1.034 | 3.902.298 | 46.679.114 | 429.032 | opus-5x517 | 58.46 |

### `d:\DEV\Projects\SPDD_puro\poc`

3 sessao(oes) - 45.080.289 tokens de entrada - 510.377 de saida - US$ 46.92

| Sessao | Inicio | Fim | Req | Input | Cache write | Cache read | Output | Modelos | Custo est. |
|---|---|---|--:|--:|--:|--:|--:|---|--:|
| `68ab90e7` | 2026-09-03 17:14 | 2026-09-03 17:42 | 21 | 42 | 292.438 | 1.120.860 | 50.323 | opus-5x21 | 3.65 |
| `84ca5e75` | 2026-09-03 17:45 | 2026-09-04 10:28 | 362 | 724 | 1.135.845 | 36.545.772 | 359.924 | opus-5x362 | 34.37 |
| `97307712` | 2026-09-04 12:12 | 2026-09-04 13:12 | 67 | 134 | 591.854 | 5.392.620 | 100.130 | opus-5x67 | 8.90 |

### `d:\DEV\Projects\SPDD_puro\poc_v2`

1 sessao(oes) - 14.256.765 tokens de entrada - 163.124 de saida - US$ 13.57

| Sessao | Inicio | Fim | Req | Input | Cache write | Cache read | Output | Modelos | Custo est. |
|---|---|---|--:|--:|--:|--:|--:|---|--:|
| `1285e682` | 2026-09-04 13:45 | 2026-09-04 14:31 | 132 | 264 | 410.070 | 13.846.431 | 163.124 | opus-5x132 | 13.57 |

### `d:\DEV\Projects\SPDD_puro\comparando_idsd`

2 sessao(oes) - 2.705.211 tokens de entrada - 34.458 de saida - US$ 3.67

| Sessao | Inicio | Fim | Req | Input | Cache write | Cache read | Output | Modelos | Custo est. |
|---|---|---|--:|--:|--:|--:|--:|---|--:|
| `a6165e90` | 2026-09-04 12:19 | 2026-09-04 13:05 | 34 | 66 | 170.225 | 1.723.005 | 18.660 | opus-5x33, <synthetic>x1 | 2.39 |
| `45c10f59` | 2026-09-04 14:27 | 2026-09-04 14:37 | 13 | 26 | 83.651 | 728.238 | 15.798 | opus-5x13 | 1.28 |

### `C:\Users\User`

2 sessao(oes) - 40.993 tokens de entrada - 96 de saida - US$ 0.26

| Sessao | Inicio | Fim | Req | Input | Cache write | Cache read | Output | Modelos | Custo est. |
|---|---|---|--:|--:|--:|--:|--:|---|--:|
| `1f23eae5` | 2026-09-02 15:17 | 2026-09-02 15:17 | 1 | 2 | 20.547 | 0 | 24 | opus-5x1 | 0.13 |
| `3f48a946` | 2026-09-02 16:30 | 2026-09-02 16:30 | 1 | 2 | 20.442 | 0 | 72 | opus-5x1 | 0.13 |

### `D:\DEV\Projects\IDSD`

1 sessao(oes) - 27.873 tokens de entrada - 42 de saida - US$ 0.18

| Sessao | Inicio | Fim | Req | Input | Cache write | Cache read | Output | Modelos | Custo est. |
|---|---|---|--:|--:|--:|--:|--:|---|--:|
| `88a96560` | 2026-09-02 10:49 | 2026-09-02 10:49 | 1 | 2 | 27.871 | 0 | 42 | opus-5x1 | 0.18 |
