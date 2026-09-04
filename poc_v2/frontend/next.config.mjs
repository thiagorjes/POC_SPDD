/** @type {import('next').NextConfig} */
const nextConfig = {
  // Runtime standalone exigido pelo Dockerfile multi-stage (ADR-008).
  output: 'standalone',
  reactStrictMode: true,
};

export default nextConfig;
