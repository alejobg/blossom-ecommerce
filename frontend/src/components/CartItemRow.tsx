import type { CartItem } from '../types/cart'
import { MAX_ITEM_QUANTITY } from '../types/cart'
import { formatPrice } from '../utils/formatPrice'

interface CartItemRowProps {
  item: CartItem
  stock: number
  disabled: boolean
  onChangeQuantity: (productId: number, quantity: number) => void
  onRemove: (productId: number) => void
}

export function CartItemRow({ item, stock, disabled, onChangeQuantity, onRemove }: CartItemRowProps) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 py-5 last:border-0">
      <div>
        <h2 className="font-semibold text-ink">{item.name}</h2>
        <p className="mt-1 text-sm text-slate-500">{formatPrice(item.price)} each</p>
        {item.quantity > stock && (
          <p className="mt-1 text-sm text-red-700">Only {stock} available. Adjust or remove this item.</p>
        )}
      </div>
      <div className="flex items-center gap-3">
        <button
          aria-label={`Decrease quantity of ${item.name}`}
          className="rounded-lg border border-slate-200 px-3 py-1 disabled:opacity-40"
          disabled={disabled || item.quantity <= 1 || stock === 0}
          onClick={() => onChangeQuantity(item.productId, Math.min(item.quantity - 1, stock))}
          type="button"
        >
          −
        </button>
        <span aria-label={`Quantity of ${item.name}`}>{item.quantity}</span>
        <button
          aria-label={`Increase quantity of ${item.name}`}
          className="rounded-lg border border-slate-200 px-3 py-1 disabled:opacity-40"
          disabled={disabled || item.quantity >= Math.min(stock, MAX_ITEM_QUANTITY)}
          onClick={() => onChangeQuantity(item.productId, item.quantity + 1)}
          type="button"
        >
          +
        </button>
        <button
          aria-label={`Remove ${item.name}`}
          className="ml-2 text-sm font-semibold text-blossom-700 underline disabled:opacity-40"
          disabled={disabled}
          onClick={() => onRemove(item.productId)}
          type="button"
        >
          Remove
        </button>
      </div>
    </li>
  )
}
