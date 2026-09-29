type EmptyStateProps = {
  title: string;
  message: string;
};

export function EmptyState({ title, message }: EmptyStateProps) {
  return (
    <div className="state-panel" role="status">
      <h2>{title}</h2>
      <p>{message}</p>
    </div>
  );
}
