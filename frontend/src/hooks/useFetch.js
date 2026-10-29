import { useCallback, useEffect, useState } from 'react'
import { errorMessage } from '../utils/format'

/** Loads data from an async function and tracks loading/error state. */
export default function useFetch(fetcher, deps = []) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setData(await fetcher())
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => { load() }, [load])
  return { data, loading, error, reload: load }
}
