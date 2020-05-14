# 스트림 처리와 점진적 입출력

IO와 ST는 그냥 명령식 프로그래밍 언어를 순수 함수적으로 표현한 것이라 모나드 안에서 명령식 프로그램을 추론하듯 해야한다.

## 명령식 입출력의 문제점을 보여주는 예제 하나

파일이 40,000 줄을 넘는지 확인하는 프로그램

```scala
def linesGt40k(filename: String): IO[Boolean] = IO {
  val src = io.Source.fromFile(filename)
  try {
    var count = 0
    val lines: Iterator[String] = src.getLines
    while ( count <= 40000 & lines.hasNext) {
      lines.next
      count += 1
    }
    count > 40000
  }
  finally src.close
}
```

