# local 환경 React 실행 방법 (조만간 배포는 할 예정)

## Available Scripts


### `npm install`
리액트 설치
### `npm run -script build`
스크립트 빌트

### `npm install react-router-dom`
라우터 설정
- 아래까지 다 하고 안될경우
- npm install react-router-dom --save

### `npm install -D tailwindcss postcss autoprefixer`
테일 윈드를 사용하기 때문에 테일윈드 세팅을 합니다.

### `npm start`
- 시작

## 파일계층
- src 
  - components :재사용 가능한 컴포넌트들이 위치하는 폴더
  - assets : 이미지 혹은 폰트와 같은 파일들이 저장, 컴포넌트 내부에서 사용하는 이미지 파일
  - hooks : 커스텀 훅
  - pages : router 등을 이요하여 라우팅 적용
  - constants : 공통적으로 사용되는 상수들을 정의한 파일
  - config : 
  - styles : css 파일들이 포함되는 폴더
  - services (= api) : 보통 api관련 로직의 모듈 파일
  - utils : 정규표현식 패턴이나 공통함수 등 공통으로 사용하는 유틸 파일
  - contexts : contextAPI를 사용할 때 관련 파일들이 위치하는곳으로 상태관리
  - App.js
  - index.js
