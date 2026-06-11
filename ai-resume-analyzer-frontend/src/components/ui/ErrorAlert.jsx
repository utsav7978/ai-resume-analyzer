export default function ErrorAlert({ message, onDismiss }) {
  if (!message) return null;

  return (
    <div className="bg-red-50 border border-red-200 rounded-lg p-4
                    flex items-start justify-between gap-3">
      <div className="flex items-start gap-2">
        <span className="text-red-500 text-lg leading-none">⚠</span>
        <p className="text-red-700 text-sm">{message}</p>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          className="text-red-400 hover:text-red-600 text-lg leading-none
                     flex-shrink-0"
        >
          ×
        </button>
      )}
    </div>
  );
}