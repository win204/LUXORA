import Link from "next/link";
import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from "react";

type BaseProps = {
  children: ReactNode;
  variant?: "primary" | "secondary" | "ghost";
};

type ButtonProps = BaseProps & ButtonHTMLAttributes<HTMLButtonElement>;

type LinkButtonProps = BaseProps &
  AnchorHTMLAttributes<HTMLAnchorElement> & {
    href: string;
  };

export function Button({ children, variant = "primary", className = "", ...props }: ButtonProps) {
  return (
    <button className={`button button-${variant} ${className}`.trim()} {...props}>
      {children}
    </button>
  );
}

export function LinkButton({
  children,
  variant = "primary",
  className = "",
  href,
  ...props
}: LinkButtonProps) {
  return (
    <Link className={`button button-${variant} ${className}`.trim()} href={href} {...props}>
      {children}
    </Link>
  );
}
