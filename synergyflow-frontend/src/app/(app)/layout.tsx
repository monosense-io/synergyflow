import type { Metadata } from "next";
import { Inter, Roboto_Mono } from "next/font/google";
import "../globals.css";
import { TanStackQueryProvider } from "@/lib/query-provider";
import { ThemeToggle } from "@/components/core/theme-toggle";
import { SessionIndicator } from "@/components/core/session-indicator";

const geistSans = Inter({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Roboto_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "SynergyFlow",
  description: "IT Service Management Platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased min-h-screen bg-background text-foreground`}
      >
        <TanStackQueryProvider>
          <div className="flex flex-col min-h-screen">
            <header className="border-b">
              <div className="container mx-auto px-4 py-4 flex justify-between items-center gap-4">
                <h1 className="text-2xl font-bold">SynergyFlow</h1>
                <div className="flex items-center gap-4">
                  <SessionIndicator />
                  <ThemeToggle />
                </div>
              </div>
            </header>
            <main className="flex-grow">
              {children}
            </main>
            <footer className="border-t py-6">
              <div className="container mx-auto px-4 text-center text-sm text-gray-500">
                © {new Date().getFullYear()} SynergyFlow. All rights reserved.
              </div>
            </footer>
          </div>
        </TanStackQueryProvider>
      </body>
    </html>
  );
}
