import { Avatar, Box } from '@chakra-ui/react'

interface UserAvatarProps {
  name?: string
  /** Размер Chakra Avatar */
  size?: 'sm' | 'md' | 'lg' | 'xl'
  /** Толщина iridescent-кольца, px */
  ring?: number
}

/** Аватар с инициалами в iridescent-кольце. */
export function UserAvatar({ name = '', size = 'sm', ring = 2 }: UserAvatarProps) {
  return (
    <Box
      display="flex"
      flexShrink={0}
      borderRadius="full"
      style={{
        padding: `${ring}px`,
        background: 'linear-gradient(135deg, #f472b6, #c084fc, #67e8f9, #fde047)',
      }}
    >
      <Avatar.Root size={size} colorPalette="purple">
        <Avatar.Fallback name={name} />
      </Avatar.Root>
    </Box>
  )
}
