import { Container } from "@/components/Container";
import { SkeletonGrid } from "@/components/Skeleton";

export default function Loading() {
  return (
    <main className="page">
      <Container>
        <SkeletonGrid />
      </Container>
    </main>
  );
}
