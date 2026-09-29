export type Brand = {
  id: string;
  name: string;
  slug: string;
};

export type Category = {
  id: string;
  name: string;
  slug: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ProductListItem = {
  id: string;
  name: string;
  slug: string;
  subtitle: string | null;
  brand: Brand;
  category: Category;
  minPrice: string | null;
  inStock: boolean;
};

export type ProductImage = {
  id: string;
  url: string;
  altText: string | null;
  displayOrder: number;
};

export type ProductSpecification = {
  id: string;
  name: string;
  value: string;
  displayOrder: number;
};

export type ProductVariant = {
  id: string;
  sku: string;
  color: string | null;
  storage: string | null;
  price: string;
  inStock: boolean;
};

export type ProductDetail = {
  id: string;
  name: string;
  slug: string;
  subtitle: string | null;
  description: string;
  brand: Brand;
  category: Category;
  images: ProductImage[];
  specifications: ProductSpecification[];
  variants: ProductVariant[];
};

export type ProductQuery = {
  page?: string;
  size?: string;
  sort?: string;
  search?: string;
  category?: string;
  brand?: string;
  minPrice?: string;
  maxPrice?: string;
};

export type CartItem = {
  itemId: string;
  variantId: string;
  productName: string;
  productSlug: string;
  sku: string;
  color: string | null;
  storage: string | null;
  quantity: number;
  unitPrice: string;
  subtotal: string;
  inStock: boolean;
};

export type Cart = {
  cartId: string;
  items: CartItem[];
  totalItems: number;
  subtotalTotal: string;
};

export type CurrentUser = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
  user: CurrentUser;
};

export type CheckoutAddress = {
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  province: string;
  country: string;
  postalCode?: string;
  promotionCode?: string;
};

export type CheckoutPreviewItem = {
  productId: string;
  variantId: string;
  slug: string;
  name: string;
  sku: string;
  color: string | null;
  storage: string | null;
  imageUrl: string | null;
  unitPrice: string;
  quantity: number;
  lineTotal: string;
};

export type CheckoutPreview = {
  items: CheckoutPreviewItem[];
  shippingAddress: CheckoutAddress;
  subtotal: string;
  shippingFee: string;
  tax: string;
  discount: string;
  grandTotal: string;
  currency: string;
  promotion: AppliedPromotion | null;
};

export type OrderItem = {
  id: string;
  productId: string;
  variantId: string;
  productSlug: string;
  productName: string;
  variantName: string | null;
  sku: string;
  color: string | null;
  storage: string | null;
  imageUrl: string | null;
  unitPrice: string;
  quantity: number;
  lineTotal: string;
};

export type OrderStatus = "PENDING" | "PAID" | "PROCESSING" | "SHIPPED" | "DELIVERED" | "CANCELLED";

export type Shipment = {
  carrier: string;
  trackingNumber: string;
  shippedAt: string;
  deliveredAt: string | null;
};

export type Order = {
  id: string;
  status: OrderStatus;
  items: OrderItem[];
  shippingAddress: CheckoutAddress;
  subtotal: string;
  shippingFee: string;
  tax: string;
  discount: string;
  grandTotal: string;
  currency: string;
  createdAt: string;
  shipment: Shipment | null;
};
export type OrderSummaryItem = {
  productId: string;
  variantId: string;
  productName: string;
  variantName: string | null;
  sku: string;
  quantity: number;
};

export type OrderSummary = {
  id: string;
  status: OrderStatus;
  grandTotal: string;
  currency: string;
  totalItems: number;
  itemPreview: OrderSummaryItem[];
  createdAt: string;
};
export type Payment = {
  id: string;
  orderId: string;
  provider: string;
  providerReference: string;
  amount: string;
  currency: string;
  status: "PENDING" | "SUCCEEDED" | "FAILED";
  createdAt: string;
  updatedAt: string;
};

export type MockPaymentOutcome = "SUCCEEDED" | "FAILED";
export type MockRefundOutcome = "SUCCEEDED" | "FAILED";

export type AdminDashboard = {
  totalProducts: number;
  totalVariants: number;
  totalOrders: number;
  pendingOrders: number;
  paidOrders: number;
};

export type AdminProductSummary = {
  id: string;
  name: string;
  slug: string;
  brandName: string;
  categoryName: string;
  active: boolean;
  variantCount: number;
  minPrice: string | null;
  inStock: boolean;
};

export type AdminProductVariant = {
  id: string;
  sku: string;
  color: string | null;
  storage: string | null;
  price: string;
  active: boolean;
  inStock: boolean;
  quantityAvailable: number;
};

export type AdminProductDetail = {
  id: string;
  name: string;
  slug: string;
  subtitle: string | null;
  description: string;
  brandId: string;
  brandName: string;
  categoryId: string;
  categoryName: string;
  active: boolean;
  images: ProductImage[];
  specifications: ProductSpecification[];
  variants: AdminProductVariant[];
};

export type AdminProductPayload = {
  name: string;
  slug: string;
  subtitle?: string;
  description: string;
  brandId: string;
  categoryId: string;
  active: boolean;
};

export type AdminVariantCreatePayload = {
  sku: string;
  color?: string;
  storage?: string;
  price: string;
  active: boolean;
  quantityAvailable: number;
};

export type AdminVariantUpdatePayload = {
  sku: string;
  color?: string;
  storage?: string;
  price: string;
  active: boolean;
};

export type AdminInventoryPayload = {
  quantityAvailable: number;
};

export type AdminImagePayload = {
  url: string;
  altText?: string;
  displayOrder: number;
};

export type AdminSpecificationPayload = {
  name: string;
  value: string;
  displayOrder: number;
};

export type AdminShipmentPayload = {
  carrier: string;
  trackingNumber: string;
};

export type AdminOrderQuery = {
  page?: number;
  size?: number;
  status?: OrderStatus;
  orderId?: string;
  customerEmail?: string;
  trackingNumber?: string;
  dateFrom?: string;
  dateTo?: string;
  sort?: "createdAt,asc" | "createdAt,desc" | "updatedAt,asc" | "updatedAt,desc";
};

export type AdminOrderNotePayload = {
  note: string;
};

export type AdminOrderSummary = {
  id: string;
  userId: string;
  userEmail: string;
  status: OrderStatus;
  grandTotal: string;
  currency: string;
  totalItems: number;
  trackingNumber: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminPaymentSummary = {
  id: string;
  provider: string;
  amount: string;
  currency: string;
  status: "PENDING" | "SUCCEEDED" | "FAILED";
  createdAt: string;
  updatedAt: string;
};

export type AdminRefundSummary = {
  id: string;
  provider: string;
  amount: string;
  currency: string;
  status: "PENDING" | "SUCCEEDED" | "FAILED";
  reason: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminOrderStatusHistory = {
  id: string;
  fromStatus: OrderStatus;
  toStatus: OrderStatus;
  changedAt: string;
  changedByUserId: string | null;
  changedByEmail: string | null;
};

export type AdminOrderDetail = Order & {
  userId: string;
  userEmail: string;
  adminNote: string | null;
  updatedAt: string;
  latestPayment: AdminPaymentSummary | null;
  latestRefund: AdminRefundSummary | null;
  statusHistory: AdminOrderStatusHistory[];
};







export type ReturnStatus = "REQUESTED" | "APPROVED" | "REJECTED" | "CANCELLED" | "RECEIVED" | "REFUNDED";

export type ReturnItem = {
  id: string;
  orderItemId: string;
  productName: string;
  sku: string;
  unitPrice: string;
  purchasedQuantity: number;
  requestedQuantity: number;
  approvedQuantity: number;
  receivedQuantity: number;
  reason: string;
};

export type ReturnStatusHistory = {
  id: string;
  fromStatus: ReturnStatus;
  toStatus: ReturnStatus;
  changedAt: string;
  changedByUserId: string | null;
  changedByEmail: string | null;
};

export type ReturnShipment = {
  carrier: string;
  trackingNumber: string;
  mockLabelReference: string;
  shippedAt: string | null;
  receivedAt: string | null;
};

export type ReturnRequest = {
  id: string;
  orderId: string;
  userId: string;
  userEmail: string;
  status: ReturnStatus;
  customerNote: string | null;
  adminNote: string | null;
  currency: string;
  estimatedRefund: string;
  receivedRefundAmount: string;
  requestedAt: string;
  approvedAt: string | null;
  rejectedAt: string | null;
  receivedAt: string | null;
  refundedAt: string | null;
  cancelledAt: string | null;
  createdAt: string;
  updatedAt: string;
  shipment: ReturnShipment | null;
  items: ReturnItem[];
  statusHistory: ReturnStatusHistory[];
};

export type ReturnSummary = {
  id: string;
  orderId: string;
  userEmail: string;
  status: ReturnStatus;
  totalRequestedItems: number;
  totalApprovedItems: number;
  totalReceivedItems: number;
  estimatedRefund: string;
  currency: string;
  trackingNumber: string | null;
  requestedAt: string;
  updatedAt: string;
};

export type AdminReturnQuery = {
  page?: number;
  size?: number;
  status?: ReturnStatus;
  orderId?: string;
  customerEmail?: string;
  trackingNumber?: string;
  dateFrom?: string;
  dateTo?: string;
  sort?: "requestedAt,desc" | "requestedAt,asc" | "updatedAt,desc" | "updatedAt,asc";
};

export type AdminReturnNotePayload = {
  note: string;
};

export type CreateReturnPayload = {
  customerNote?: string;
  items: Array<{ orderItemId: string; quantity: number; reason: string }>;
};

export type AdminReturnQuantityPayload = {
  returnItemId: string;
  quantity: number;
};

export type AdminApproveReturnPayload = {
  adminNote?: string;
  items?: AdminReturnQuantityPayload[];
};

export type AdminReceiveReturnPayload = {
  adminNote?: string;
  items?: AdminReturnQuantityPayload[];
};

export type AdminRejectReturnPayload = {
  adminNote: string;
};

export type ReturnRefundPayload = {
  mockOutcome?: MockRefundOutcome;
  reason?: string;
};

export type AppliedPromotion = { code: string; name: string; discountAmount: string; };
export type AdminPromotion = { id: string; code: string; name: string; description: string | null; type: "PERCENTAGE" | "FIXED_AMOUNT"; value: string; minimumOrderAmount: string | null; maximumDiscountAmount: string | null; startsAt: string; endsAt: string; usageLimit: number | null; usageCount: number; active: boolean; createdAt: string; updatedAt: string; };
export type AdminPromotionPayload = Omit<AdminPromotion, "id" | "usageCount" | "createdAt" | "updatedAt">;
