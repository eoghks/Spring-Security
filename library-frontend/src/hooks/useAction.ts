import { useCallback } from 'react';
import { errorMessage } from '../api/client';
import { useNotice } from '../components/Notice';

/**
 * "요청 → 성공 안내 + 후처리 / 실패 안내" 공통 처리.
 * @returns 성공 여부
 */
export function useAction(onDone?: () => void) {
  const { notify } = useNotice();
  return useCallback(
    async (task: () => Promise<unknown>, successMessage: string): Promise<boolean> => {
      try {
        await task();
        notify(successMessage, 'success');
        onDone?.();
        return true;
      } catch (e) {
        notify(errorMessage(e), 'error');
        return false;
      }
    },
    [notify, onDone],
  );
}
