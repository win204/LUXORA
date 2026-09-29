"use client";

import { Container } from "@/components/Container";
import { ErrorState } from "@/components/ErrorState";
import { Button } from "@/components/Button";

export default function Error({ reset }: { reset: () => void }) {
  return (
    <main className="page">
      <Container>
        <ErrorState />
        <div className="center-action">
          <Button type="button" onClick={reset}>
            Try again
          </Button>
        </div>
      </Container>
    </main>
  );
}
