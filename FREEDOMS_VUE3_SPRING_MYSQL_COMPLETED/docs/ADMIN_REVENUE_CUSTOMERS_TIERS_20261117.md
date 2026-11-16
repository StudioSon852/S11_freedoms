# Admin Revenue, Customer Directory and Member Tiers (17/11/2026)

## Database update

For an existing MySQL database, back up the database first, then run:

`database/05_CUSTOMER_TIERS_ADDRESS_REPORTS.sql`

The migration:
- Adds `users.default_address` only if the column is missing.
- Upserts the five configured membership tiers and discount rates.
- Recalculates existing CUSTOMER tier assignments from `users.total_spent`.
- Does not delete customer, order, payment, refund, product, or image data.

Keep `spring.jpa.hibernate.ddl-auto=validate`; do not switch it to `create` or `create-drop`.

## Customer tiers

| Tier | Cumulative spending | Discount on subsequent orders |
|---|---:|---:|
| Mới | 0đ | 0% |
| Bạc | 100.000đ | 2% |
| Vàng | 250.000đ | 3% |
| Bạch kim | 500.000đ | 4% |
| Kim cương | 1.000.000đ | 5% |

Spend is increased when a customer order is recorded as paid/completed. A successful refund reduces the customer's cumulative spend by the actually refunded amount and triggers tier recalculation. A tier earned by an order applies on the next order after that paid/completed order updates the cumulative spend.

The pricing pipeline remains: variant discount, then member tier discount, then coupon. This lets the existing Online and POS checkouts use the same server-side tier percentage.

## API additions/updates

- `GET /api/admin/overview`: summary and eight latest orders.
- `GET /api/admin/revenue?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD&customer=...&orderCode=...`: paid completed sales; confirmed refunds are deducted from the revenue total.
- `GET /api/admin/orders?date=YYYY-MM-DD&orderCode=...&status=...`: Admin/Manager order/payment filtering.
- `GET /api/pos/orders?date=YYYY-MM-DD&orderCode=...&status=...`: POS order filtering within the current staff scope.
- `GET /api/admin/customers?q=...&tier=...` and `GET /api/pos/customers?q=...&tier=...`: customer search and tier filter.
- `GET /api/admin/customers/{id}` and `GET /api/pos/customers/{id}`: customer profile, default address, cumulative spend, tier rate, order count and up to ten recent orders.
- `GET/PATCH /api/account/me`: returns/saves the customer's default address with the profile fields.

## UI locations

- Admin: `/admin/overview` (revenue filters + recent orders), `/admin/orders`, `/admin/customers`.
- Employee: `/employee/orders`, `/employee/customers`; Customers appears in the horizontal employee navigation.
- Customer: `/account` allows editing the default address; checkout pre-fills it when the field is empty.

## Acceptance checklist

1. Run migration 05, then start backend; Hibernate validation should find `users.default_address`.
2. Open Admin Overview; eight recent orders appear when order data exists.
3. Filter revenue by from/to date, customer name/phone/email, and partial/full order code; totals should match returned order rows after confirmed partial refunds are deducted.
4. Search and filter Admin orders by date, order code, order state and payment state.
5. Repeat order filtering in Employee Orders; only permitted POS orders are returned.
6. Open Customer Directory in Admin and Employee; search by name/email/phone/address and filter each membership tier.
7. Open a customer detail; verify contact data, default address, total accumulated spend, discount and recent orders.
8. Edit the customer's default address in `/account`, save, then re-open the directory detail to verify it appears.
9. With cumulative spending below 100.000đ, a new order should use Mới 0%; after successful paid/completed spend reaches 100.000đ, next order uses Bạc 2%.
10. Repeat at 250.000đ (Vàng 3%), 500.000đ (Bạch kim 4%) and 1.000.000đ (Kim cương 5%).
11. Complete a partial refund; verify customer total spend decreases by the refunded amount and tier recalculates when a threshold is crossed.
