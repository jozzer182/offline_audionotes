#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface WhisperBridge : NSObject

// Initialize with path to the ggml model file
- (nullable instancetype)initWithModelPath:(NSString *)modelPath;

// Transcribe audio file at path, returning text
// language can be "auto", "en", "es", etc.
- (nullable NSString *)transcribeAudioAtPath:(NSString *)audioPath language:(NSString *)language;

@end

NS_ASSUME_NONNULL_END
