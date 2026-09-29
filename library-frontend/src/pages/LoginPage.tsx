import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errorCode, errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

/** 접속 조건(IP·기간·요일·시간) 위반 안내 — 구체 사유는 서버 로그에만 남는다 */
const ACCESS_CONDITION_DENIED_MESSAGE = '허용된 접속 환경이 아닙니다. 관리자에게 문의하세요.';

/** 로그인 실패 안내 문구 */
function loginErrorMessage(error: unknown): string {
  return errorCode(error) === 'ACCESS_CONDITION_DENIED'
    ? ACCESS_CONDITION_DENIED_MESSAGE
    : errorMessage(error, '로그인에 실패했습니다.');
}

/** 로그인 제출 상태 */
function useLoginSubmit() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (username: string, password: string) => {
    setSubmitting(true);
    setError('');
    try {
      await login(username, password);
      navigate('/', { replace: true });
    } catch (e) {
      setError(loginErrorMessage(e));
    } finally {
      setSubmitting(false);
    }
  };
  return { submit, error, submitting };
}

/** 로그인 화면 */
export default function LoginPage() {
  const { me } = useAuth();
  const { submit, error, submitting } = useLoginSubmit();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  if (me) {
    return <Navigate to="/" replace />;
  }
  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    submit(username, password);
  };

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={onSubmit}>
        <h1>도서관 대출 시스템</h1>
        <label>
          아이디
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </label>
        <label>
          비밀번호
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password" required />
        </label>
        {error && <p className="error-text">{error}</p>}
        <button className="btn primary" disabled={submitting}>{submitting ? '로그인 중…' : '로그인'}</button>
        <p className="muted">
          계정이 없나요? <Link to="/signup">회원가입</Link>
        </p>
      </form>
    </div>
  );
}
