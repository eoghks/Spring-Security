import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

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
      setError(errorMessage(e, '로그인에 실패했습니다.'));
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
