# redilock
redisson을 이용한 간편한 메소드 락 제공


ex) 아래처럼 사용 
@Sync(key = "somethingId", limit = 100) 
