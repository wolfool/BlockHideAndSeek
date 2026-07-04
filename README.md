# BlockHideAndSeek

스타일 블럭 숨바꼭질 Minecraft 플러그인 (Paper 1.21.4)

## 기능
- 🟩 도망자 → 블럭으로 변신 (BlockDisplay, 외부 플러그인 불필요)
- ⬛ Shift 웅크리기 → 격자(Grid)에 맞게 진짜 블럭처럼 고정 (이동 가능)
- 💣 술래 힌트 명령어 `/bhs hint` → 모든 도망자 머리 위 폭죽 발사
- ⚙️ 힌트 횟수, 게임 시간 등 `config.yml` 에서 설정 가능
- 🎒 `/bhs setkit hider|seeker` 로 GUI에서 시작 키트 설정
- 📊 보스바 + 사이드바 스코어보드 실시간 표시
- 🛡️ 게임 중 블럭 파괴/설치, 아이템 드롭 방지
- 🚪 탈주자 자동 탈락 처리

## 명령어

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/bhs start` | 게임 시작 | OP |
| `/bhs stop` | 게임 강제 종료 | OP |
| `/bhs setlobby` | 로비 스폰 설정 | OP |
| `/bhs sethider` | 도망자 스폰 설정 | OP |
| `/bhs setseeker` | 술래 스폰 설정 | OP |
| `/bhs setkit [hider\|seeker]` | 시작 키트 GUI 설정 | OP |
| `/bhs reload` | config 리로드 | OP |
| `/bhs hint` | 힌트 폭죽 사용 | 술래 |

## config.yml

```yaml
times:
  hide-time: 60    # 숨기 시간(초)
  game-time: 300   # 술래잡기 시간(초)

hints:
  max-usages: 3    # 게임당 힌트 사용 횟수

selectable-blocks:
  - BOOKSHELF
  - OAK_LOG
  - DIAMOND_ORE
  # ... 원하는 블럭 추가
```

## 빌드

```bash
./gradlew build
```

빌드 결과물: `build/libs/BlockHideAndSeek-1.0-SNAPSHOT.jar`

## 요구사항
- Paper 1.21.4 이상
- Java 21 이상
