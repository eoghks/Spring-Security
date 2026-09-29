import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage, fieldErrors } from '../api/client';
import { authApi } from '../api/endpoints';

interface SignupForm {
  username: string;
  password: string;
  passwordConfirm: string;
  name: string;
  email: string;
}

const EMPTY: SignupForm = { username: '', password: '', passwordConfirm: '', name: '', email: '' };

const FIELDS: { name: keyof SignupForm; label: string; type: string }[] = [
  { name: 'username', label: '아이디', type: 'text' },
  { name: 'password', label: '비밀번호', type: 'password' },
  { name: 'passwordConfirm', label: '비밀번호 확인', type: 'password' },
  { name: 'name', label: '이름', type: 'text' },
  { name: 'email', label: '이메일', type: 'email' },
];

/** 서버와 같은 규칙으로 먼저 검사해 즉시 안내한다(최종 판단은 서버) */
function validate(form: SignupForm): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!/^[a-z0-9_]{4,20}$/.test(form.username)) {
    errors.username = '아이디는 영문 소문자·숫자·밑줄 4~20자입니다.';
  }
  if (form.password.length < 8 || form.password.length > 64) {
    errors.password = '비밀번호는 8~64자입니다.';
  } else if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password) || !/[^A-Za-z\d]/.test(form.password)) {
    errors.password = '비밀번호는 영문·숫자·특수문자를 모두 포함해야 합니다.';
  }
  if (form.password !== form.passwordConfirm) {
    errors.passwordConfirm = '비밀번호가 일치하지 않습니다.';
  }
  if (!form.name.trim()) {
    errors.name = '이름을 입력하세요.';
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '이메일 형식이 올바르지 않습니다.';
  }
  return errors;
}

/** 회원가입 화면. 가입하면 일반 회원 역할이 부여된다 */
export default function SignupPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<SignupForm>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState('');

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const clientErrors = validate(form);
    setErrors(clientErrors);
    setServerError('');
    if (Object.keys(clientErrors).length > 0) {
      return;
    }
    try {
      await authApi.signup({ username: form.username, password: form.password, name: form.name, email: form.email });
      navigate('/login', { replace: true });
    } catch (e) {
      setErrors(fieldErrors(e));
      setServerError(errorMessage(e, '회원가입에 실패했습니다.'));
    }
  };

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={onSubmit} noValidate>
        <h1>회원가입</h1>
        {FIELDS.map(({ name, label, type }) => (
          <label key={name}>
            {label}
            <input type={type} value={form[name]} aria-invalid={Boolean(errors[name])}
              onChange={(e) => setForm({ ...form, [name]: e.target.value })} />
            {errors[name] && <span className="error-text">{errors[name]}</span>}
          </label>
        ))}
        {serverError && <p className="error-text">{serverError}</p>}
        <button className="btn primary">가입하기</button>
        <p className="muted">
          이미 계정이 있나요? <Link to="/login">로그인</Link>
        </p>
      </form>
    </div>
  );
}
