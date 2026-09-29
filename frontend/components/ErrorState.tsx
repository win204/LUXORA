type ErrorStateProps = {
  title?: string;
  message?: string;
};

export function ErrorState({
  title = "Something went wrong",
  message = "The catalog could not be loaded right now."
}: ErrorStateProps) {
  return (
    <div className="state-panel state-error" role="alert">
      <h2>{title}</h2>
      <p>{message}</p>
    </div>
  );
}
