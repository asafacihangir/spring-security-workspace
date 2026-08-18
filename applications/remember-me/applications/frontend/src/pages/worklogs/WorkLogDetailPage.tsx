import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getWorkLog, type WorkLog } from '../../services/worklogs'

export default function WorkLogDetailPage() {
  const { workLogId } = useParams()
  const [workLog, setWorkLog] = useState<WorkLog | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    getWorkLog(Number(workLogId))
      .then(setWorkLog)
      .catch(() => setError(true))
  }, [workLogId])

  if (error) return <div className="alert alert-danger">Work log not found.</div>
  if (!workLog) return <p>Loading…</p>

  return (
    <>
      <h1>Work Log #{workLog.id}</h1>
      <dl className="row">
        <dt className="col-sm-3">Created</dt>
        <dd className="col-sm-9">{workLog.createdDate}</dd>
        <dt className="col-sm-3">Created By</dt>
        <dd className="col-sm-9">{workLog.createdBy}</dd>
        <dt className="col-sm-3">Explanation</dt>
        <dd className="col-sm-9">{workLog.explanation}</dd>
      </dl>
      <Link to="/work-logs/my">Back to My Work Logs</Link>
    </>
  )
}
