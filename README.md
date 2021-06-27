# redilock
redisson을 이용한 간편한 메소드 락 제공


# Sample
// 객체내의 필드를 이용하는 경우, 필드명을 이용하여 사용 가능
@Sync(key = "dto.user.userId", limit = 100)  
public void method(RequestDTO dto) { .... };   
