type PriceProps = {
  amount: string | null;
  prefix?: string;
};

const formatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD"
});

export function Price({ amount, prefix }: PriceProps) {
  if (!amount) {
    return <span className="price">Price unavailable</span>;
  }

  return (
    <span className="price">
      {prefix}
      {formatter.format(Number(amount))}
    </span>
  );
}
