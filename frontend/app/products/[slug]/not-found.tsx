import { Container } from "@/components/Container";
import { EmptyState } from "@/components/EmptyState";
import { LinkButton } from "@/components/Button";

export default function ProductNotFound() {
  return (
    <main className="page">
      <Container>
        <EmptyState title="Product not found" message="This product is not available in the catalog." />
        <div className="center-action">
          <LinkButton href="/shop">Return to shop</LinkButton>
        </div>
      </Container>
    </main>
  );
}
