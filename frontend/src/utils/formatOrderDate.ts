const formatter = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
})

export function formatOrderDate(date: string): string {
  return formatter.format(new Date(date))
}
