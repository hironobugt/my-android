// Lambda関数をExpressルートとして変換するヘルパー
const lambdaToExpress = (lambdaHandler) => {
    return async (req, res) => {
        try {
            // Lambda event形式に変換
            const event = {
                httpMethod: req.method,
                path: req.path,
                pathParameters: req.params,
                queryStringParameters: req.query,
                headers: req.headers,
                body: JSON.stringify(req.body),
                requestContext: {
                    authorizer: req.user ? { claims: req.user } : null
                }
            };

            // Lambda関数を実行
            const result = await lambdaHandler(event);
            
            // レスポンスを返す
            res.status(result.statusCode);
            
            if (result.headers) {
                Object.keys(result.headers).forEach(key => {
                    res.set(key, result.headers[key]);
                });
            }
            
            // サムネイルエンドポイントの特別処理
            if (result.isBase64Encoded && result.body) {
                const buffer = Buffer.from(result.body, 'base64');
                res.send(buffer);
            } else {
                const body = typeof result.body === 'string' ? JSON.parse(result.body) : result.body;
                res.json(body);
            }
            
        } catch (error) {
            console.error('Lambda handler error:', error);
            res.status(500).json({ error: 'Internal server error' });
        }
    };
};