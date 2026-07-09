import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const DayPlanActionsContext = createContext({
  actions: {},
  setActions: () => {},
  clearActions: () => {},
});

export function DayPlanActionsProvider({ children }) {
  const [actions, setCurrentActions] = useState({});

  const setActions = useCallback((nextActions) => {
    setCurrentActions(nextActions || {});
  }, []);

  const clearActions = useCallback(() => {
    setCurrentActions({});
  }, []);

  const value = useMemo(() => ({ actions, setActions, clearActions }), [actions, clearActions, setActions]);

  return (
    <DayPlanActionsContext.Provider value={value}>
      {children}
    </DayPlanActionsContext.Provider>
  );
}

export function useDayPlanActions() {
  return useContext(DayPlanActionsContext);
}
