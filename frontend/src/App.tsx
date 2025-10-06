import { ConfigProvider, Typography } from 'antd'

const { Title, Paragraph } = Typography

function App() {
  return (
    <ConfigProvider>
      <div style={{ padding: '48px', maxWidth: '1200px', margin: '0 auto' }}>
        <Title level={1}>SynergyFlow</Title>
        <Paragraph>Enterprise ITSM & PM Platform - Coming Soon</Paragraph>
      </div>
    </ConfigProvider>
  )
}

export default App
