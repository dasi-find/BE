# AI 수색카드 분석 내부 API 계약

BE와 AI 서버 사이에서 사용하는 내부 계약입니다. 외부 클라이언트는 이 API를
직접 호출하지 않습니다.

## 역할 분리

- BE: 사용자 인증, 요청 검증, 이미지 소유권 확인, Presigned URL 생성, 결과 저장
- AI: GPT 전처리, 이미지·텍스트 모델 실행, 표준 분석 JSON 생성

BE는 특정 모델에 의존하지 않습니다. AI 서버는 GPT, SigLIP2, BGE-M3 등의
구성을 독립적으로 변경하고 실제 실행 구성을 `modelVersion`으로 식별합니다.

## 분석 요청

```http
POST /internal/v1/search-card-analyses
Content-Type: application/json
```

```json
{
  "category": "WALLET",
  "itemName": "남색 카드지갑",
  "colors": ["NAVY"],
  "brand": null,
  "featureDescription": "앞면 중앙에 은색 로고가 있어요.",
  "images": [
    {
      "imageId": 501,
      "imageUrl": "https://private-bucket.example/presigned-url",
      "imageType": "ACTUAL"
    }
  ],
  "lostDate": "2026-08-17",
  "lostStartTime": "18:00:00",
  "lostEndTime": "20:00:00",
  "lostLocation": {
    "placeName": "판교역",
    "address": "경기도 성남시 분당구 판교역로 166",
    "latitude": 37.3947,
    "longitude": 127.1112,
    "description": null
  }
}
```

사진은 선택 사항이며 사진이 없으면 `images`는 빈 배열입니다. `imageUrl`은
비공개 S3 객체를 읽을 수 있는 1시간 Presigned URL이므로 AI 서버는 결과를
반환하기 전에 이미지를 내려받아야 합니다. URL을 로그나 영구 저장소에 남기지
않습니다.

`imageType`은 사용자가 직접 촬영한 사진인 `ACTUAL` 또는 동일 제품 참고 사진인
`REFERENCE`입니다.

## 성공 응답

```json
{
  "category": "WALLET",
  "itemName": "CARD_WALLET",
  "colors": ["NAVY", "BLACK"],
  "brand": null,
  "materials": ["LEATHER"],
  "ocrText": null,
  "features": ["앞면 은색 로고", "오른쪽 아래 긁힘"],
  "modelVersion": "preprocess-v1"
}
```

필수 문자열인 `category`, `itemName`, `modelVersion`은 공백일 수 없습니다.
`colors`, `materials`, `features`는 값이 없더라도 빈 배열로 반환하며 `null`을
반환하지 않습니다. `brand`와 `ocrText`만 `null`을 허용합니다.

## 오류 처리

- AI 서버가 `503`을 반환하거나 연결·읽기 시간 초과가 발생하면 BE는 `AI5031`을 반환합니다.
- 그 밖의 AI 오류, 역직렬화 실패, 계약에 맞지 않는 응답은 BE가 `AI5021`로 변환합니다.
- BE는 실패한 분석 결과를 DB에 저장하지 않습니다.

현재 BE 연결 제한은 연결 3초, 전체 응답 대기 30초입니다. 모델 처리 시간이
30초를 안정적으로 넘는다면 동기식 외부 API를 비동기 작업 방식으로 변경하는
별도 계약이 필요합니다.
